@echo off
echo 🤖 Starting AI Camera Search Service...
echo.

REM Check if Python is installed
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Python is not installed or not in PATH
    echo Please install Python 3.8+ from https://python.org
    pause
    exit /b 1
)

REM Check if virtual environment exists
if not exist "venv" (
    echo 📦 Creating virtual environment...
    python -m venv venv
)

REM Activate virtual environment
echo 🔧 Activating virtual environment...
call venv\Scripts\activate.bat

REM Install dependencies
echo 📚 Installing Python dependencies...
pip install -r ai-service\requirements.txt

REM Check for CUDA
echo 🔍 Checking for CUDA support...
python -c "import torch; print(f'✅ CUDA available: {torch.cuda.is_available()}') if torch.cuda.is_available() else print('⚠️  CUDA not available. Using CPU (slower but functional)')"

REM Start the AI service
echo.
echo 🚀 Starting AI Camera Search Service...
echo 📍 Service will be available at: http://localhost:5000
echo 🔍 Health check: http://localhost:5000/health
echo.
echo Press Ctrl+C to stop the service
echo.

cd ai-service
python app.py

pause
