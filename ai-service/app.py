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
        
        # Decode base64 image
        image_data = base64.b64decode(data['image'])
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

@app.route('/embeddings/similarity-search', methods=['POST'])
def similarity_search():
    """Find similar products based on embedding similarity"""
    try:
        data = request.get_json()
        
        if not data or 'query_embedding' not in data:
            return jsonify({"error": "Query embedding is required"}), 400
        
        query_embedding_str = data['query_embedding']
        limit = data.get('limit', 20)
        
        # Convert string embedding back to numpy array
        query_embedding = np.array([float(x) for x in query_embedding_str.split(',')])
        
        if len(product_embeddings) == 0:
            logger.warning("No product embeddings available for search")
            return jsonify({"similar_product_ids": []})
        
        # Calculate similarities
        similarities = []
        for product_id, stored_embedding_str in product_embeddings.items():
            stored_embedding = np.array([float(x) for x in stored_embedding_str.split(',')])
            
            # Calculate cosine similarity
            similarity = cosine_similarity(
                query_embedding.reshape(1, -1),
                stored_embedding.reshape(1, -1)
            )[0][0]
            
            similarities.append((product_id, similarity))
        
        # Sort by similarity and get top results
        similarities.sort(key=lambda x: x[1], reverse=True)
        top_similar_ids = [int(pid) for pid, _ in similarities[:limit]]
        
        logger.info(f"Found {len(top_similar_ids)} similar products")
        
        return jsonify({
            "similar_product_ids": top_similar_ids,
            "similarities": [sim for _, sim in similarities[:limit]]
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
