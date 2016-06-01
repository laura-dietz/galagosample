package sample.galago;


import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.parse.Tag;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.core.retrieval.RetrievalFactory;
import org.lemurproject.galago.core.retrieval.ScoredDocument;
import org.lemurproject.galago.core.retrieval.query.Node;
import org.lemurproject.galago.core.retrieval.query.StructuredQuery;
import org.lemurproject.galago.core.tokenize.Tokenizer;
import org.lemurproject.galago.core.util.WordLists;
import org.lemurproject.galago.utility.Parameters;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *  Minimal Example using Galago search API.
 *
 *  There are more parameters and configuration options discusses on the Secret Galago Documentation
 *  https://medium.com/@lauradietz100/galago-the-secret-documentation-7e1c1b205dda
 *
 *  @author John Foley
 *  @author Laura Dietz
 */
public class GalagoCheatSheet {
    public static void main(String[] args) throws Exception {
        Parameters globalParams;

        // one way of configuring the Searcher
        // create JSON file search.params with following content
//        {
//            "index":"/path/to/your/index"
//        }
//        String jsonConfigFile = "search.params";
//        globalParams = Parameters.parseFile(jsonConfigFile);

        // other way of configuring searcher
        globalParams = Parameters.create();
        globalParams.set("index", "/..../index/wikipedia"); // an alternative way to select the index
        globalParams.set("defaultSmoothingMu",2500.0); // all queries use Dirichlet smoothing by default - this is the mu parameter
        // alternatively you can set "mu" in query params



        Retrieval retrieval = RetrievalFactory.create(globalParams);

        String qlQuery = "who let the dogs out";   // this is a query likelihood query
        String sdmQuery = "#sdm( who let the dogs out )";   // this is a sequential dependence model query
        String sdmRmQuery = "#rm( #sdm( who let the dogs out ) )";   //  this is an rm3 expanded SDM model
        String weigthedQLquery = "#combine:0=0.4:1=0.1:2=0.1:3=1.0( who let the dogs out )";   // this is a query likelihood query with manual weights
        // by default all queries are smoothed with Dirichlet smoothing using corpus statistics



        String query = sdmRmQuery;

        Parameters p = Parameters.instance();
        p.set("startAt", 0);
        p.set("resultCount", 50);    // ask for 50 results
        p.set("requested", 50);
        p.set("mu",2000.0); // query specific Dirichlet smoothing mu (alternatively set the defaultSmoothingMu property in the global Params
        Node root = StructuredQuery.parse(query);       // turn the query string into a query tree
        Node transformed = retrieval.transformQuery(root, p);  // apply traversals

        List<ScoredDocument> ranking = retrieval.executeQuery(transformed, p).scoredDocuments; // issue the query!

        for(ScoredDocument sd: ranking) {
            System.out.println(sd.rank + " " + sd.documentName + " (" + sd.score + ")");

            // pull the document
            Document document = retrieval.getDocument(sd.documentName, new Document.DocumentComponents(true, true, true));
            // with the three boolean flags you can configure whether you need terms, fulltext, and metadata.

            // fulltext of the document, as indexed
            String fulltext = document.text;
            // meta data in attribute -> value format
            final Map<String, String> metadata = document.metadata;

            // fetching tokenized terms
            final List<String> terms = document.terms;

            // character offsets corresponding to the terms in the fulltext.
            final List<Integer> termCharBegin = document.termCharBegin;
            final List<Integer> termCharEnd = document.termCharEnd;
            System.out.println("\t\tterms.get(3) = " + terms.get(3));
            System.out.println("\t\t3rd term:" + terms.get(3) + "  char offsets:" + termCharBegin.get(3) + "-" + termCharEnd.get(3) + " -- in full text: " + fulltext.substring(termCharBegin.get(3), termCharEnd.get(3)));


            // if you create a fielded index (marked with pseudo-HTML syntax), you may locate their positions with the tags
            final List<Tag> tags = document.tags;
            for (Tag tag : tags) {
                if (tag.name.equals("title")) {      // which tag names are available depends on your index
                    final List<String> termsInField = terms.subList(tag.begin, tag.end + 1); // tag.end is inclusive offset!
                    final String textInField = fulltext.substring(tag.charBegin, tag.charEnd);
                    System.out.println("\t\t title = "+textInField);
                }
            }
        }



        // Normalize text the same way Galago does by default
        System.out.println("normalize text with galago's default tokenizer");

        final Tokenizer tokenizer = Tokenizer.create(Parameters.create());
        final List<String> tokenizedTerms = tokenizer.tokenize("My various-Formatted text~string. !?").terms;
        for(String tok:tokenizedTerms){
            System.out.println(tok);
        }

        System.out.println("after removing stopwords:");

        // remove stopwords
        final Set<String> stopwords = WordLists.getWordList("inquery");
        for(String tok:tokenizedTerms){
            if(!stopwords.contains(tok)) {
                System.out.println(tok);
            }
        }

    }

}
