JFLAGS = -g
JC = javac
JVM = sudo java
RM = rm -f
DIR = edu/wisc/cs/sdn/simpledns/
File=
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java
CLASSES = \
	edu/wisc/cs/sdn/simpledns/SimpleDNS.java \
	edu/wisc/cs/sdn/simpledns/packet/DNS.java \
	edu/wisc/cs/sdn/simpledns/packet/DNSQuestion.java \
	edu/wisc/cs/sdn/simpledns/packet/DNSResourceRecord.java 

MAIN = $(DIR)SimpleDNS

default: classes

classes: $(CLASSES:.java=.class)

run: classes
	$(JVM) $(MAIN) -r a.root-servers.net -e ec2.csv

clean:
	$(RM) **/*.class

