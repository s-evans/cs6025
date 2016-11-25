
JAVA_FILES := $(wildcard *.java)

JAVA_CLASSES := $(JAVA_FILES:.java=.class)

.PHONY: all

all: run

.PHONY: run

run: $(JAVA_CLASSES)
	java -cp . H24 < test21.txt

%.class: %.java
	javac $<

