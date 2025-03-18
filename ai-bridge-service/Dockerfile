FROM python:3.9-slim

WORKDIR /app

RUN apt-get update && \
    apt-get install -y curl && \
    curl -fsSL https://ollama.com/install.sh | sh

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

RUN mkdir -p uploads

ENV NVIDIA_API_KEY=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
ENV PORT=5000

RUN ollama pull llama3.2

EXPOSE 5000

CMD ["python", "app.py"]