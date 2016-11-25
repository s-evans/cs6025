
JAVA_FILES := $(wildcard *.java)

JAVA_CLASSES := $(JAVA_FILES:.java=.class)

.PHONY: all

all: $(JAVA_CLASSES)

%.class: %.java
	javac $<

