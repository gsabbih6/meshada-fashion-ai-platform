import requests
import json

token = "EAATCYu9Elv8BR81F35HYjBQpOAPvY0wupDJC8yIPUnIZBM6aZAONvEWwiOVxD0BsMvZB1lQ2cGytvmZA1EfXCar5ZAohPwwB7CFbsNrTyQinjCPIKQlNOMqwRJXZBnZBUs5Q0yR1TWtO6b8v9bZBDiVlkbAva0jdpWTFisY3LnC772tXKHqi8wzfmyg05h3uGCH1OH8ZD"
business_account_id = "17841464487692741"

print(f"Checking Media for Instagram Business Account: {business_account_id}...")
url = f"https://graph.facebook.com/v19.0/{business_account_id}/media?fields=id,caption,media_type,permalink,timestamp&access_token={token}"

try:
    res = requests.get(url)
    print(f"Status: {res.status_code}")
    print(json.dumps(res.json(), indent=2))
except Exception as e:
    print("ERROR:", e)
