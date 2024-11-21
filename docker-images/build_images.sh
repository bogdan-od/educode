#!/bin/bash

# Оголошуємо асоціативний масив
declare -A dockerfiles=(
    ["DockerfileAsm"]="assembler-compiler:latest"
    ["DockerfileCppWithGmp"]="cpp-with-gmp-compiler:14.2"
    ["DockerfileDotnet"]="dotnet-compiler:8.0"
    ["DockerfileJava"]="java-compiler:23"
    ["DockerfileNode"]="node-compiler:22.11.0"
    ["DockerfilePypy"]="pypy-compiler:3.10"
    ["DockerfileRust"]="rust-compiler:1.82.0"
    ["DockerfileC"]="c-compiler:10.2"
    ["DockerfileDart"]="dart-compiler:3.5"
    ["DockerfileGo"]="go-compiler:1.20"
    ["DockerfileKotlin"]="kotlin-compiler:2.0.21"
    ["DockerfilePerl"]="perl-compiler:5.32"
    ["DockerfilePython"]="python-compiler:3.11"
    ["DockerfileSwift"]="swift-compiler:5.6"
    ["DockerfileCpp"]="cpp-compiler:14.2"
    ["DockerfileDgdc"]="d-gdc-compiler:14.2"
    ["DockerfileHaskell"]="haskell-compiler:8.8"
    ["DockerfileMono"]="mono-compiler:6.12"
    ["DockerfilePHP"]="php-compiler:8.2"
    ["DockerfileRuby"]="ruby-compiler:3.3"
    ["DockerfileLua"]="lua-compiler:latest"
    ["DockerfilePascal"]="pascal-compiler:latest"
)

# Цикл по асоціативному масиву
for dockerfile in "${!dockerfiles[@]}"; do
    image_name="${dockerfiles[$dockerfile]}"
    
    # Перевірка існування файлу Dockerfile
    if [ -f "$dockerfile" ]; then
        echo "Building Docker image: $image_name from $dockerfile"
        # Збираємо образ
        docker build -t "$image_name" -f "$dockerfile" .
    else
        echo "Warning: $dockerfile does not exist. Skipping."
    fi
done