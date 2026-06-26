import psycopg2
from psycopg2.extras import RealDictCursor

db_url = "postgresql://postgres:PtVlXBRzAXAcbMvchNWpBpZoTvyhcwbU@reseau.proxy.rlwy.net:39661/railway"

try:
    conn = psycopg2.connect(db_url)
    cursor = conn.cursor(cursor_factory=RealDictCursor)
    
    # Query social_comments
    cursor.execute("SELECT * FROM social_comments;")
    comments = cursor.fetchall()
    
    print("SOCIAL COMMENTS COUNT:", len(comments))
    for c in comments:
        print(c)
        
    cursor.close()
    conn.close()
except Exception as e:
    print("ERROR:", e)
