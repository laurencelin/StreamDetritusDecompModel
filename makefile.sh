#!/bin/bash
rm *.class
javac -cp cmdParser_4jan2013.jar:Pattern_4jan2013.jar:TimePatternC_04jun2013.jar:. Version4dRun.java -Xlint:unchecked
jar cfm version4dII_28Nov2017.jar manifest *class *java *jar
jar tf version4dII_28Nov2017.jar
