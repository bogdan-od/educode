#!/bin/sh

# Файл конфігурації, який зберігає мову та версію
CONFIG_FILE="config.txt"

# Отримуємо дію з першого аргументу
ACTION=$1

if [ -f "$CONFIG_FILE" ]; then
    # Зчитуємо мову та версію з файлу
    read -r LANGUAGE LANG_VERSION < "$CONFIG_FILE"
else
    # Якщо файл не існує, запитуємо у користувача
    LANGUAGE=$3
    LANG_VERSION=$4

    # Зберігаємо мову та версію у файл
    echo "$LANGUAGE $LANG_VERSION" > "$CONFIG_FILE"
fi

# Визначаємо файли вхідного коду та вихідного файлу в залежності від мови програмування
case "$LANGUAGE" in
    "assembler")
    	FILE="main.asm"
    	OUTPUT="./main"
    	;;
    "c")
        FILE="main.c"
        OUTPUT="./main"
        ;;
    "cpp" | "cpp_gmp")
        FILE="main.cpp"
        OUTPUT="./main"
        ;;
    "java")
        FILE="Main.java"
        OUTPUT="Main"
        ;;
    "rust")
        FILE="main.rs"
        OUTPUT="./main"
        ;;
    "kotlin")
        FILE="Main.kt"
        OUTPUT="Main.jar"
        ;;
    "d_gdc")
        FILE="main.d"
        OUTPUT="./main"
        ;;
    "go")
        FILE="main.go"
        OUTPUT="./main"
        ;;
    "python" | "pypy")
        FILE="main.py"
        OUTPUT="main.py"
        ;;
    "javascript")
        FILE="main.js"
        OUTPUT="main.js"
        ;;
    "ruby")
        FILE="main.rb"
        OUTPUT="main.rb"
        ;;
    "php")
        FILE="main.php"
        OUTPUT="main.php"
        ;;
    "perl")
        FILE="main.pl"
        OUTPUT="main.pl"
        ;;
    "haskell")
        FILE="Main.hs"
        OUTPUT="./Main"
        ;;
    "dart")
        FILE="main.dart"
        OUTPUT="main.dart"
        ;;
    "swift")
        FILE="main.swift"
        OUTPUT="./main"
        ;;
    "csharp_dotnet")
        FILE="Program.cs"
        OUTPUT="./program/bin/Debug/net$LANG_VERSION/program"
        ;;
    "csharp_mono")
    	FILE="Program.cs"
    	OUTPUT="./Program.exe"
    	;;
    "lua")
    	FILE="main.lua"
	OUTPUT="main.lua"
    	;;
    "pascal_fpc")
    	FILE="main.pas"
    	OUTPUT="./Program"
    	;;
    *)
        echo "Непідтримувана мова програмування: $LANGUAGE"
        exit 1
        ;;
esac

# Створюємо файл з кодом, якщо він не існує
if [ ! -f "$FILE" ]; then
	echo "$2" > "$FILE"
fi

# Обробка команд збірки та запуску
case "$ACTION" in
    "build")
        # Компіляція коду в залежності від мови програмування
        case $LANGUAGE in
		"assembler")
		    nasm -f elf64 -o output.o $FILE
		    ld -o $OUTPUT output.o
		    ;;
		"c")
		    gcc -std="c$LANG_VERSION" $FILE -o $OUTPUT
		    ;;
		"cpp")
		    g++ -std="c++$LANG_VERSION" $FILE -o $OUTPUT
		    ;;
		"cpp_gmp")
		    g++ -std="c++$LANG_VERSION" $FILE -lgmp -o $OUTPUT
		    ;;
		"csharp_dotnet")
		    dotnet new console -n program
		    mv $FILE program/Program.cs
		    (cd program && dotnet build)
		    ;;
		"csharp_mono")
		    mcs $FILE
		    ;;
		"d_gdc")
		    gdc $FILE -o $OUTPUT
		    ;;
		"dart")
		    ;;
		"go")
		    go build $FILE
		    ;;
		"haskell")
		    ghc $FILE -o $OUTPUT
		    ;;
		"java")
		    javac $FILE
		    ;;
		"javascript")
		    ;;
		"lua")
		    ;;
    "pascal_fpc")
        fpc -o$OUTPUT $FILE
        ;;
		"perl")
		    ;;
		"php")
		    ;;
		"python" | "pypy")
		    ;;
		"ruby")
		    ;;
		"rust")
		    rustc $FILE
		    ;;
		"swift")
		    swiftc $FILE -o $OUTPUT
		    ;;
		*)
		    echo "Мова не підтримується."
		    exit 1
		    ;;
	    esac
        ;;
    "run")
        # Запуск програми в залежності від мови програмування
        case "$LANGUAGE" in
            "c" | "cpp" | "cpp_gmp" | "rust" | "d_gdc" | "go" | "swift" | "csharp_dotnet" | "assembler" | "pascal_fpc")
                $OUTPUT
                ;;
            "java")
            	java $OUTPUT;
            	;;
    	    "kotlin")
    	    	java -jar $OUTPUT
    	    	;;
	    "csharp_mono")
	    	mono $OUTPUT
	    	;;
            "python")
                python "$FILE"
                ;;
            "pypy")
            	pypy "$FILE"
            	;;
            "javascript")
                node "$FILE"
                ;;
            "ruby")
                ruby "$FILE"
                ;;
            "php")
                php "$FILE"
                ;;
            "perl")
                perl "$FILE"
                ;;
            "haskell")
                runhaskell "$FILE"
                ;;
            "dart")
                dart "$FILE"
                ;;
            "lua")
            	lua "$FILE"
                ;;
            *)
                ;;
        esac
        ;;
    *)
        echo "Невірна дія. Використовуйте 'build' або 'run'."
        exit 1
        ;;
esac