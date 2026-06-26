import psycopg2
from psycopg2.extras import RealDictCursor

db_url = "postgresql://postgres:PtVlXBRzAXAcbMvchNWpBpZoTvyhcwbU@reseau.proxy.rlwy.net:39661/railway"

try:
    conn = psycopg2.connect(db_url)
    cursor = conn.cursor(cursor_factory=RealDictCursor)
    
    # List all tables in the public schema
    cursor.execute("""
        SELECT table_name 
        FROM information_schema.tables 
        WHERE table_schema = 'public';
    """)
    tables = cursor.fetchall()
    print("TABLES:")
    for t in tables:
        print("-", t['table_name'])
        
    cursor.close()
    conn.close()
except Exception as e:
    print("ERROR:", e)
