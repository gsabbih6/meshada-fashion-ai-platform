import psycopg2

db_url = "postgresql://postgres:PtVlXBRzAXAcbMvchNWpBpZoTvyhcwbU@reseau.proxy.rlwy.net:39661/railway"

try:
    conn = psycopg2.connect(db_url)
    cursor = conn.cursor()
    cursor.execute("SELECT current_database(), current_user, current_schema();")
    db_info = cursor.fetchone()
    print("Database Info:", db_info)
    
    # Check if there are other schemas
    cursor.execute("SELECT schema_name FROM information_schema.schemata;")
    schemas = cursor.fetchall()
    print("Schemas:", schemas)
    
    cursor.close()
    conn.close()
except Exception as e:
    print("ERROR:", e)
