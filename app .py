from flask import Flask, request, jsonify, send_from_directory
import sqlite3
import os

BASE = os.path.dirname(os.path.abspath(__file__))
DB_PATH = os.path.join(BASE, 'headshoterz.db')
ADMIN_PASS = 'lobao123'

app = Flask(__name__, static_folder=BASE, static_url_path='')

# ============ BANCO ============
def init_db():
    with sqlite3.connect(DB_PATH) as conn:
        conn.execute('''
            CREATE TABLE IF NOT EXISTS pedidos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT NOT NULL,
                id_jogo TEXT NOT NULL,
                funcao TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'pendente'
                       CHECK(status IN ('pendente','aceito','recusado')),
                criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        ''')
        conn.commit()

def db_fetch(sql, params=()):
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    try:
        return [dict(r) for r in conn.execute(sql, params).fetchall()]
    finally:
        conn.close()

def db_execute(sql, params=()):
    conn = sqlite3.connect(DB_PATH)
    try:
        cur = conn.execute(sql, params)
        conn.commit()
        return cur.lastrowid
    finally:
        conn.close()

# ============ ROTAS ============
@app.route('/')
def index():
    return send_from_directory(BASE, 'index.html')

@app.route('/style.css')
def css():
    return send_from_directory(BASE, 'style.css', mimetype='text/css')

@app.route('/script.js')
def js():
    return send_from_directory(BASE, 'script.js', mimetype='application/javascript')

@app.route('/api/pedidos', methods=['POST'])
def criar_pedido():
    data = request.get_json(force=True) or {}
    nome = (data.get('nome') or '').strip()
    id_jogo = str(data.get('idJogo') or '').strip()
    funcao = data.get('funcao')

    if not nome or not id_jogo or funcao not in ('Full Gas', 'Cura', 'Rush'):
        return jsonify({'error': 'Dados inválidos'}), 400

    novo = db_execute(
        'INSERT INTO pedidos (nome, id_jogo, funcao) VALUES (?, ?, ?)',
        (nome, id_jogo, funcao)
    )
    return jsonify({'ok': True, 'id': novo}), 201

@app.route('/api/pedidos', methods=['GET'])
def listar_pedidos():
    rows = db_fetch('SELECT * FROM pedidos ORDER BY id DESC')
    return jsonify(rows)

@app.route('/api/pedidos/<int:pid>', methods=['PATCH'])
def atualizar(pid):
    data = request.get_json(force=True) or {}
    novo = data.get('status')
    if novo not in ('aceito', 'recusado', 'pendente'):
        return jsonify({'error': 'Status inválido'}), 400
    db_execute('UPDATE pedidos SET status=? WHERE id=?', (novo, pid))
    return jsonify({'ok': True})

@app.route('/api/login', methods=['POST'])
def login():
    data = request.get_json(force=True) or {}
    if (data.get('senha') or '') == ADMIN_PASS:
        return jsonify({'ok': True})
    return jsonify({'ok': False}), 401

# ============ START ============
if __name__ == '__main__':
    init_db()
    print('🔥 HEADSHOTERZ → http://localhost:5000')
    app.run(host='0.0.0.0', port=5000, debug=True)