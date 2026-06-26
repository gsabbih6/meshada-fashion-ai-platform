import psycopg2
from psycopg2.extras import RealDictCursor

db_url = "postgresql://postgres:PtVlXBRzAXAcbMvchNWpBpZoTvyhcwbU@reseau.proxy.rlwy.net:39661/railway"

try:
    conn = psycopg2.connect(db_url)
    cursor = conn.cursor(cursor_factory=RealDictCursor)
    
    cursor.execute("SELECT * FROM social_comments;")
    comments = cursor.fetchall()
    print("COMMENTS:")
    for c in comments:
        print(c)
        
    cursor.execute("SELECT platform, access_token FROM social_credentials;")
    creds = cursor.fetchall()
    print("CREDENTIALS:")
    for cr in creds:
        print(cr['platform'], cr['access_token'][:15] + "...")
        
    cursor.close()
    conn.close()
except Exception as e:
    print("ERROR:", e)
