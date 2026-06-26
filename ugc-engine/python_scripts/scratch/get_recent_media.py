import requests
import json

token = "EAATCYu9Elv8BR81F35HYjBQpOAPvY0wupDJC8yIPUnIZBM6aZAONvEWwiOVxD0BsMvZB1lQ2cGytvmZA1EfXCar5ZAohPwwB7CFbsNrTyQinjCPIKQlNOMqwRJXZBnZBUs5Q0yR1TWtO6b8v9bZBDiVlkbAva0jdpWTFisY3LnC772tXKHqi8wzfmyg05h3uGCH1OH8ZD"
business_account_id = "17841464487692741"

print(f"Fetching recent media and checking comments...")
url = f"https://graph.facebook.com/v19.0/{business_account_id}/media?fields=id,caption,media_type,permalink,timestamp&limit=5&access_token={token}"

try:
    res = requests.get(url)
    media_list = res.json().get("data", [])
    for m in media_list:
        media_id = m["id"]
        permalink = m["permalink"]
        caption = m.get("caption", "")[:50]
        print(f"\nMedia ID: {media_id} | Permalink: {permalink} | Caption: {caption}...")
        
        # Get comments for this media
        comments_url = f"https://graph.facebook.com/v19.0/{media_id}/comments?fields=id,text,username,timestamp&access_token={token}"
        comments_res = requests.get(comments_url)
        comments = comments_res.json().get("data", [])
        print(f"  Comments count: {len(comments)}")
        for c in comments:
            print(f"    - @{c.get('username')}: {c.get('text')} ({c.get('timestamp')})")
            
except Exception as e:
    print("ERROR:", e)
