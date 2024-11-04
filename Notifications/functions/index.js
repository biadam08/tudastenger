const functions = require('firebase-functions');
const { SecretManagerServiceClient } = require('@google-cloud/secret-manager');
const admin = require('firebase-admin');
const axios = require("axios");
admin.initializeApp();

const client = new SecretManagerServiceClient();

exports.sendFriendRequestNotification = functions.firestore
    .document('FriendRequests/{requestId}')
    .onCreate((snap, context) => {
        const newRequest = snap.data();
        const senderUid = newRequest.user_uid1;
        const recipientUid = newRequest.user_uid2;

        return admin.firestore().collection('Users').doc(senderUid).get()
            .then(senderDoc => {
                if (senderDoc.exists) {
                    const senderName = senderDoc.data().username;

                    return admin.firestore().collection('Users').doc(recipientUid).get()
                        .then(recipientDoc => {
                            if (recipientDoc.exists && recipientDoc.data().fcmToken) {
                                const token = recipientDoc.data().fcmToken;

								const message = {
									data: {
										title: 'Tudástenger',
										body: `Új barátkérelem: ${senderName} barátnak jelölt!`,
										type: "friendRequest"
									},
									token: token,
								};

                                return admin.messaging().send(message)
                                    .then(response => {
                                        console.log('Értesítés sikeresen elküldve: ', response);
                                    })
                                    .catch(error => {
                                        console.log('Értesítés küldése sikertelen: ', error);
                                    });
                            } else {
                                console.log('A címzett nem létezik vagy nincs FCM token.');
                                return null;
                            }
                        });
                }
            });
    });
	
exports.sendDuelRequestNotification = functions.firestore
    .document('Duels/{requestId}')
    .onCreate((snap, context) => {
        const newRequest = snap.data();
        const senderUid = newRequest.challengerUid;
        const recipientUid = newRequest.challengedUid;

        return admin.firestore().collection('Users').doc(senderUid).get()
            .then(senderDoc => {
                if (senderDoc.exists) {
                    const senderName = senderDoc.data().username;

                    return admin.firestore().collection('Users').doc(recipientUid).get()
                        .then(recipientDoc => {
                            if (recipientDoc.exists && recipientDoc.data().fcmToken) {
                                const token = recipientDoc.data().fcmToken;

								const message = {
									data: {
										title: 'Tudástenger',
                                        body: `${senderName} kihívott egy párbajra, játsszd le mielőbb!`,
										type: "duelRequest"
									},
									token: token,
								};
					
                                return admin.messaging().send(message)
                                    .then(response => {
                                        console.log('Értesítés sikeresen elküldve: ', response);
                                    })
                                    .catch(error => {
                                        console.log('Értesítés küldése sikertelen: ', error);
                                    });
                            } else {
                                console.log('A címzett nem létezik vagy nincs FCM token.');
                                return null;
                            }
                        });
                }
            });
    });


exports.getChatGPTResponse = functions.https.onCall(async (data, context) => {
  try {
    const [version] = await client.accessSecretVersion({
      name: 'projects/tudastenger-2fa34/secrets/chatgpt_api_key/versions/latest',
    });

    const apiKey = version.payload.data.toString('utf8');

    const prompt = data.prompt;

     const url = "https://api.openai.com/v1/chat/completions";

    const headers = {
      "Content-Type": "application/json",
      "Authorization": `Bearer ${apiKey}`,
    };

    const body = {
      model: 'gpt-4o-mini',
      messages: [
        { role: 'user', content: prompt },
      ],
      max_tokens: 350,
      temperature: 0.7
    };

    const response = await axios.post(url, body, { headers });

    if (response.status === 200) {
      return { response: response.data.choices[0].message.content };
    } else {
      throw new functions.https.HttpsError("internal", "Request failed.");
    }
  } catch (error) {
    throw new functions.https.HttpsError("internal", error.message);
  }
});