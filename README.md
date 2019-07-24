# LOD-Community-Detection

This is the JAVA source code of our ISWC paper [Detecting Erroneous Identity Links on the Web using Network Metrics](https://www.cs.vu.nl/~frankh/postscript/ISWC2018.pdf).

#### Goal of the experiments:

In this work, we show how network metrics such as the community structure of the owl:sameAs graph can be used in order to detect possibly erroneous identity statements.
For detecting the community structure inside each equality set, we use the Louvain algorithm.
Using the resulted communities, we assign an error degree to each owl:sameAs link. 
This error degree is a value between 0.0 (possibly correct link) and 1.0 (possibly erroneous).

 
#### This code requires two external resources for replicating our experiments in the paper:

> 1. Download the [sameAs.cc dataset](https://zenodo.org/record/1973099).

This data set contains 558.9 million owl:sameAs links collected from the 2015 LOD Laundromat crawl of over 650K data documents from the Web. It is exposed in a single HDT file that is 5GB in size, and is publicly accessible via an [LDF interface](https://krr.triply.cc/krr/sameas). 

> 2. Download the [Equivalence Classes](https://zenodo.org/record/3345674).

This data set of equivalence classes results from the closure of all 558 million owl:sameAs links in the sameAs.cc data set. 

#### All necessary resources and results are also available in our [sameAs.cc](http://sameas.cc) Identity Web service.
