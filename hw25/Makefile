
JAVA_FILES := $(wildcard *.java)

JAVA_CLASSES := $(JAVA_FILES:.java=.class)

.PHONY: all

all: run

.PHONY: run

run: $(JAVA_CLASSES)
	java -cp . H25B test25 33
	java -cp . H25C hall_cif.yuv 33

%.class: %.java
	javac $<

