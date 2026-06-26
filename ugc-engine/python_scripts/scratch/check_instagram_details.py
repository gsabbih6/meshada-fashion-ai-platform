import requests
import json

token = "EAATCYu9Elv8BR81F35HYjBQpOAPvY0wupDJC8yIPUnIZBM6aZAONvEWwiOVxD0BsMvZB1lQ2cGytvmZA1EfXCar5ZAohPwwB7CFbsNrTyQinjCPIKQlNOMqwRJXZBnZBUs5Q0yR1TWtO6b8v9bZBDiVlkbAva0jdpWTFisY3LnC772tXKHqi8wzfmyg05h3uGCH1OH8ZD"
page_id = "127085577040955"

print(f"Checking Subscriptions for Page: (ID: {page_id})")
sub_url = f"https://graph.facebook.com/v19.0/{page_id}/subscribed_apps?access_token={token}"

try:
    sub_res = requests.get(sub_url)
    print(f"Status: {sub_res.status_code}")
    print(json.dumps(sub_res.json(), indent=2))
except Exception as e:
    print("ERROR:", e)
