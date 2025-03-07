FROM python:3.10-slim

WORKDIR /app

RUN apt-get update && apt-get install -y wget
RUN wget https://github.com/ollama/ollama/releases/download/v0.1.26/ollama-linux-amd64 -O /usr/local/bin/ollama && \
    chmod +x /usr/local/bin/ollama

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY ./app ./app

RUN ollama serve & sleep 5 && ollama pull llama3.2

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]