import psycopg2

db_url = "postgresql://postgres:PtVlXBRzAXAcbMvchNWpBpZoTvyhcwbU@reseau.proxy.rlwy.net:39661/railway"

try:
    conn = psycopg2.connect(db_url)
    cursor = conn.cursor()
    cursor.execute("SELECT COUNT(*) FROM social_comments;")
    count = cursor.fetchone()[0]
    print("TOTAL COMMENTS:", count)
    cursor.close()
    conn.close()
except Exception as e:
    print("ERROR:", e)
