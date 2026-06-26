import requests
import json

token = "EAATCYu9Elv8BR81F35HYjBQpOAPvY0wupDJC8yIPUnIZBM6aZAONvEWwiOVxD0BsMvZB1lQ2cGytvmZA1EfXCar5ZAohPwwB7CFbsNrTyQinjCPIKQlNOMqwRJXZBnZBUs5Q0yR1TWtO6b8v9bZBDiVlkbAva0jdpWTFisY3LnC772tXKHqi8wzfmyg05h3uGCH1OH8ZD"
page_id = "127085577040955"

print(f"Subscribing Page (ID: {page_id}) to App...")
url = f"https://graph.facebook.com/v19.0/{page_id}/subscribed_apps"

# We should subscribe to feed and comments/messages/etc. if supported.
# For Instagram comments, subscribing to 'feed' on the Facebook page is usually sufficient/required,
# and page/instagram connection delivers instagram webhooks.
payload = {
    "subscribed_fields": "feed,mention,messages",
    "access_token": token
}

try:
    res = requests.post(url, data=payload)
    print(f"Status: {res.status_code}")
    print(json.dumps(res.json(), indent=2))
except Exception as e:
    print("ERROR:", e)
