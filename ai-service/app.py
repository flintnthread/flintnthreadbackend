"""
Professional AI Service for Camera Search
Uses CLIP model for image embeddings and similarity search
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import torch
import torch.nn.functional as F
from PIL import Image
import clip
import numpy as np
import base64
import io
from typing import List, Dict, Any
import logging
from sklearn.metrics.pairwise import cosine_similarity

app = Flask(__name__)
CORS(app)

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Load CLIP model
DEVICE = "cuda" if torch.cuda.is_available() else "cpu"
MODEL_NAME = "ViT-B/32"
model, preprocess = clip.load(MODEL_NAME, device=DEVICE)

# In-memory storage for product embeddings (in production, use database)
product_embeddings = {}
product_ids = []

@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    return jsonify({
        "status": "healthy",
        "model": MODEL_NAME,
        "device": DEVICE,
        "loaded_products": len(product_embeddings)
    })

@app.route('/embeddings/generate', methods=['POST'])
def generate_embedding():
    """Generate embedding for an image"""
    try:
        data = request.get_json()
        
        if not data or 'image' not in data:
            return jsonify({"error": "Image data is required"}), 400
        
        raw_image = data['image']
        if isinstance(raw_image, str) and ',' in raw_image and raw_image.strip().startswith('data:'):
            raw_image = raw_image.split(',', 1)[1]
        
        # Decode base64 image
        image_data = base64.b64decode(raw_image)
        image = Image.open(io.BytesIO(image_data))
        
        # Ensure image is in RGB format
        if image.mode != 'RGB':
            image = image.convert('RGB')
        
        # Preprocess and generate embedding
        image_tensor = preprocess(image).unsqueeze(0).to(DEVICE)
        
        with torch.no_grad():
            embedding = model.encode_image(image_tensor)
            embedding = F.normalize(embedding, p=2, dim=1)
        
        # Convert to list for JSON serialization
        embedding_list = embedding.cpu().numpy().flatten().tolist()
        
        return jsonify({
            "embedding": embedding_list,
            "model": MODEL_NAME,
            "dimension": len(embedding_list)
        })
        
    except Exception as e:
        logger.error(f"Error generating embedding: {str(e)}")
        return jsonify({"error": str(e)}), 500


def _parse_embedding(value):
    """Accept CSV string or numeric list from Spring / clients."""
    if value is None:
        raise ValueError("embedding is required")
    if isinstance(value, list):
        return np.array([float(x) for x in value], dtype=float)
    if isinstance(value, str):
        parts = [p.strip() for p in value.split(',') if p.strip()]
        return np.array([float(x) for x in parts], dtype=float)
    raise ValueError("embedding must be a CSV string or list of numbers")


@app.route('/embeddings/similarity-search', methods=['POST'])
def similarity_search():
    """Find similar products based on embedding similarity"""
    try:
        data = request.get_json()
        
        if not data or 'query_embedding' not in data:
            return jsonify({"error": "Query embedding is required"}), 400
        
        query_embedding = _parse_embedding(data['query_embedding'])
        limit = int(data.get('limit', 20) or 20)
        
        if len(product_embeddings) == 0:
            logger.warning("No product embeddings available for search")
            return jsonify({"similar_product_ids": []})
        
        # Calculate similarities
        similarities = []
        for product_id, stored_embedding_str in product_embeddings.items():
            stored_embedding = _parse_embedding(stored_embedding_str)
            if stored_embedding.shape != query_embedding.shape:
                continue
            
            # Calculate cosine similarity
            similarity = cosine_similarity(
                query_embedding.reshape(1, -1),
                stored_embedding.reshape(1, -1)
            )[0][0]
            
            similarities.append((product_id, float(similarity)))
        
        # Sort by similarity and keep only confident matches (avoid random top-K on noise).
        MIN_SIMILARITY = float(data.get('min_similarity', 0.32) or 0.32)
        similarities.sort(key=lambda x: x[1], reverse=True)
        top = [(pid, sim) for pid, sim in similarities if sim >= MIN_SIMILARITY][:limit]
        top_similar_ids = [int(pid) for pid, _ in top]
        
        logger.info(f"Found {len(top_similar_ids)} similar products above {MIN_SIMILARITY}")
        
        return jsonify({
            "similar_product_ids": top_similar_ids,
            "similarities": [sim for _, sim in top]
        })
        
    except Exception as e:
        logger.error(f"Error in similarity search: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route('/embeddings/store', methods=['POST'])
def store_embedding():
    """Store embedding for a product"""
    try:
        data = request.get_json()
        
        if not data or 'product_id' not in data or 'embedding' not in data:
            return jsonify({"error": "Product ID and embedding are required"}), 400
        
        product_id = data['product_id']
        embedding_str = ','.join(map(str, data['embedding']))
        
        product_embeddings[product_id] = embedding_str
        
        logger.info(f"Stored embedding for product {product_id}")
        
        return jsonify({
            "message": "Embedding stored successfully",
            "product_id": product_id,
            "total_embeddings": len(product_embeddings)
        })
        
    except Exception as e:
        logger.error(f"Error storing embedding: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route('/embeddings/bulk-store', methods=['POST'])
def bulk_store_embeddings():
    """Store multiple embeddings at once"""
    try:
        data = request.get_json()
        
        if not data or 'embeddings' not in data:
            return jsonify({"error": "Embeddings data is required"}), 400
        
        stored_count = 0
        for item in data['embeddings']:
            if 'product_id' in item and 'embedding' in item:
                product_id = item['product_id']
                embedding_str = ','.join(map(str, item['embedding']))
                product_embeddings[product_id] = embedding_str
                stored_count += 1
        
        logger.info(f"Bulk stored {stored_count} embeddings")
        
        return jsonify({
            "message": "Bulk embeddings stored successfully",
            "stored_count": stored_count,
            "total_embeddings": len(product_embeddings)
        })
        
    except Exception as e:
        logger.error(f"Error in bulk store: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route('/embeddings/stats', methods=['GET'])
def get_stats():
    """Get statistics about stored embeddings"""
    return jsonify({
        "total_embeddings": len(product_embeddings),
        "model": MODEL_NAME,
        "device": DEVICE,
        "product_ids": list(product_embeddings.keys())
    })

if __name__ == '__main__':
    logger.info(f"Starting AI Service on port 5000 with model {MODEL_NAME}")
    logger.info(f"Using device: {DEVICE}")
    app.run(host='0.0.0.0', port=5000, debug=True)
