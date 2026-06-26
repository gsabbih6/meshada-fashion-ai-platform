import requests
import json

app_id = "1339630083675903"
app_secret = "b9d3e1755c5b4267aa892536b98e8ecd"
app_token = f"{app_id}|{app_secret}"

url = f"https://graph.facebook.com/v19.0/{app_id}?fields=id,name,link,category&access_token={app_token}"

try:
    res = requests.get(url)
    print(json.dumps(res.json(), indent=2))
except Exception as e:
    print("ERROR:", e)
