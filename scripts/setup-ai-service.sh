#!/bin/bash

# Setup Script for AI Camera Search Service
echo "🚀 Setting up AI Camera Search Service..."

# Check if Python is installed
if ! command -v python3 &> /dev/null; then
    echo "❌ Python 3 is required but not installed."
    exit 1
fi

# Check if pip is installed
if ! command -v pip3 &> /dev/null; then
    echo "❌ pip3 is required but not installed."
    exit 1
fi

# Create virtual environment if it doesn't exist
if [ ! -d "venv" ]; then
    echo "📦 Creating virtual environment..."
    python3 -m venv venv
fi

# Activate virtual environment
echo "🔧 Activating virtual environment..."
source venv/bin/activate

# Install dependencies
echo "📚 Installing Python dependencies..."
pip install -r ai-service/requirements.txt

# Check if CUDA is available (optional)
echo "🔍 Checking for CUDA support..."
python3 -c "
import torch
if torch.cuda.is_available():
    print(f'✅ CUDA is available! Found {torch.cuda.device_count()} GPU(s)')
    for i in range(torch.cuda.device_count()):
        print(f'   GPU {i}: {torch.cuda.get_device_name(i)}')
else:
    print('⚠️  CUDA not available. Using CPU (slower but functional)')
"

# Create startup script
echo "📝 Creating startup script..."
cat > start-ai-service.sh << 'EOF'
#!/bin/bash
cd "$(dirname "$0")"
source venv/bin/activate
cd ai-service
echo "🤖 Starting AI Camera Search Service..."
echo "📍 Service will be available at: http://localhost:5000"
echo "🔍 Health check: http://localhost:5000/health"
python app.py
EOF

chmod +x start-ai-service.sh

# Create database migration script
echo "🗄️  Creating database migration script..."
cat > migrate-embeddings.sql << 'EOF'
-- Create table for product embeddings
CREATE TABLE IF NOT EXISTS product_embeddings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    embedding_vector TEXT NOT NULL,
    model_version VARCHAR(50) DEFAULT 'clip-vit-base-patch32',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_product_id (product_id),
    INDEX idx_active (is_active),
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- Add index for faster similarity search (optional, for production)
-- CREATE INDEX idx_embedding_vector_gin ON product_embeddings USING gin(embedding_vector);
EOF

echo ""
echo "✅ Setup completed successfully!"
echo ""
echo "🎯 Next Steps:"
echo "1. Run the database migration:"
echo "   mysql -u root -p flintandthread < migrate-embeddings.sql"
echo ""
echo "2. Start the AI service:"
echo "   ./start-ai-service.sh"
echo ""
echo "3. Start your Spring Boot application"
echo ""
echo "4. Test the camera search in your mobile app"
echo ""
echo "📚 For more details, see: ai-service/README.md"
