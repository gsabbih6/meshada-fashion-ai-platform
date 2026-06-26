import psycopg2
from psycopg2.extras import RealDictCursor

db_url = "postgresql://postgres:PtVlXBRzAXAcbMvchNWpBpZoTvyhcwbU@reseau.proxy.rlwy.net:39661/railway"

try:
    conn = psycopg2.connect(db_url)
    cursor = conn.cursor(cursor_factory=RealDictCursor)
    
    cursor.execute("SELECT id, item_name, affiliate_link FROM ugc_videos;")
    videos = cursor.fetchall()
    print("VIDEOS COUNT:", len(videos))
    for v in videos:
        print(v)
        
    cursor.close()
    conn.close()
except Exception as e:
    print("ERROR:", e)
