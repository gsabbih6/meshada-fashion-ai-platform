import requests

token = "EAATCYu9Elv8BR81F35HYjBQpOAPvY0wupDJC8yIPUnIZBM6aZAONvEWwiOVxD0BsMvZB1lQ2cGytvmZA1EfXCar5ZAohPwwB7CFbsNrTyQinjCPIKQlNOMqwRJXZBnZBUs5Q0yR1TWtO6b8v9bZBDiVlkbAva0jdpWTFisY3LnC772tXKHqi8wzfmyg05h3uGCH1OH8ZD"
url = f"https://graph.facebook.com/debug_token?input_token={token}&access_token={token}"

try:
    response = requests.get(url)
    print("DEBUG TOKEN:")
    print(response.json())
except Exception as e:
    print("ERROR:", e)
