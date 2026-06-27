# AI Service for Camera Search

Professional AI-powered image similarity search using CLIP model.

## Features

- **CLIP Model**: Uses OpenAI's CLIP ViT-B/32 for image understanding
- **Vector Embeddings**: Converts images to 512-dimensional vectors
- **Similarity Search**: Finds visually similar products using cosine similarity
- **REST API**: Clean RESTful interface for Spring Boot integration
- **GPU Support**: Automatic CUDA detection and usage

## Setup

1. Install Python dependencies:
```bash
pip install -r requirements.txt
```

2. Start the service:
```bash
python app.py
```

The service will start on `http://localhost:5000`

## API Endpoints

### Generate Embedding
```
POST /embeddings/generate
Content-Type: application/json

{
  "image": "base64_encoded_image",
  "model": "ViT-B/32"
}
```

### Similarity Search
```
POST /embeddings/similarity-search
Content-Type: application/json

{
  "query_embedding": "0.123,0.456,0.789,...",
  "limit": 20
}
```

### Store Product Embedding
```
POST /embeddings/store
Content-Type: application/json

{
  "product_id": 123,
  "embedding": [0.123, 0.456, 0.789, ...]
}
```

### Health Check
```
GET /health
```

## Integration with Spring Boot

The Spring Boot application automatically communicates with this service for:
- Generating embeddings when products are uploaded
- Finding similar products during camera search
- Managing product embeddings

## Performance

- **Embedding Generation**: ~50ms per image (GPU), ~200ms per image (CPU)
- **Similarity Search**: ~10ms for 10k products
- **Memory Usage**: ~100MB for 10k product embeddings

## Production Considerations

1. **Database Storage**: Replace in-memory storage with PostgreSQL + pgvector
2. **Caching**: Add Redis for frequent searches
3. **Load Balancing**: Run multiple instances behind a load balancer
4. **Model Updates**: Support for model versioning and hot-swapping
