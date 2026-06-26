import psycopg2

db_url = "postgresql://postgres:PtVlXBRzAXAcbMvchNWpBpZoTvyhcwbU@reseau.proxy.rlwy.net:39661/railway"

try:
    conn = psycopg2.connect(db_url)
    cursor = conn.cursor()
    
    cursor.execute("SELECT id, product_link FROM social_comments;")
    rows = cursor.fetchall()
    for row in rows:
        row_id = row[0]
        link = row[1]
        print(f"Comment ID: {row_id}, Link: {link}")
        if link and "unisex-tank-top1.html" in link:
            new_link = link.replace("unisex-tank-top1.html", "unisex-jersey-tank-top")
            cursor.execute("UPDATE social_comments SET product_link = %s WHERE id = %s;", (new_link, row_id))
            print(f"  -> Updated to: {new_link}")
            
    conn.commit()
    cursor.close()
    conn.close()
except Exception as e:
    print("ERROR:", e)
