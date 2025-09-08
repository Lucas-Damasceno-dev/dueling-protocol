#!/bin/bash
echo ">>> Construindo o projeto com Docker..."
docker build -t dueling-protocol .
echo ">>> Build concluído! A imagem Docker está pronta para uso."
