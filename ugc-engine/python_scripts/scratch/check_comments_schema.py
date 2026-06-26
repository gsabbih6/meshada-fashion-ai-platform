import psycopg2
from psycopg2.extras import RealDictCursor

db_url = "postgresql://postgres:PtVlXBRzAXAcbMvchNWpBpZoTvyhcwbU@reseau.proxy.rlwy.net:39661/railway"

try:
    conn = psycopg2.connect(db_url)
    cursor = conn.cursor(cursor_factory=RealDictCursor)
    
    # Query social_comments column names
    cursor.execute("SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'social_comments';")
    columns = cursor.fetchall()
    
    print("SOCIAL COMMENTS COLUMNS:")
    for col in columns:
        print(f"{col['column_name']}: {col['data_type']}")
        
    cursor.close()
    conn.close()
except Exception as e:
    print("ERROR:", e)
