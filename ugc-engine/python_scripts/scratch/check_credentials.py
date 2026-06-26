import psycopg2
from psycopg2.extras import RealDictCursor

db_url = "postgresql://postgres:PtVlXBRzAXAcbMvchNWpBpZoTvyhcwbU@reseau.proxy.rlwy.net:39661/railway"

try:
    conn = psycopg2.connect(db_url)
    cursor = conn.cursor(cursor_factory=RealDictCursor)
    
    # Query social_credentials
    cursor.execute("SELECT platform, access_token, business_account_id, updated_at FROM social_credentials;")
    creds = cursor.fetchall()
    
    print("SOCIAL CREDENTIALS COUNT:", len(creds))
    for c in creds:
        # Mask the access token for security but show if it's there
        token = c.get('access_token')
        masked = token[:15] + "..." if token else None
        print(f"Platform: {c.get('platform')}, Token: {masked}, BusinessAccount: {c.get('business_account_id')}, Updated: {c.get('updated_at')}")
        
    cursor.close()
    conn.close()
except Exception as e:
    print("ERROR:", e)
