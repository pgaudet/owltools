package owltools.cli;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;

import owltools.cli.tools.CLIMethod;
import owltools.gaf.GafDocument;
import owltools.gaf.GafObjectsBuilder;
import owltools.solrj.FlexSolrDocumentLoader;
import owltools.solrj.GafSolrDocumentLoader;
import owltools.solrj.OntologySolrLoader;

/**
 *  Solr/GOlr loading.
 */
public class SolrCommandRunner extends GafCommandRunner {

	private static final Logger LOG = Logger.getLogger(SolrCommandRunner.class);
	
	private String globalSolrURL = null;
	
	/**
	 * Set an optional Solr URL to use with Solr options so they don't have to
	 * be specified separately for every option.
	 * 
	 * @param opts
	 */
	@CLIMethod("--solr-url")
	public void setSolrUrl(Opts opts) {
		globalSolrURL = opts.nextOpt(); // shift it off of null
		LOG.info("Globally use GOlr server at: " + globalSolrURL);
	}
	
	/**
	 * Manually purge the index to try again.
	 * Since this cascade is currently ordered, can be used to purge before we load.
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--purge-solr")
	public void purgeSolr(Opts opts) throws Exception {

		// Check to see if the global url has been set, otherwise use the local one.
		String url = sortOutSolrURL(opts, globalSolrURL);				

		// Wipe out the solr index at url.
		SolrServer server = new CommonsHttpSolrServer(url);
		try {
			server.deleteByQuery("*:*");
		} catch (SolrServerException e) {
			LOG.info("Purge at: " + url + " failed!");
			e.printStackTrace();
		}
	}
	
	/**
	 * Used for loading whatever ontology stuff we have into GOlr.
	 * 
	 * @param opts 
	 * @throws Exception
	 */
	@CLIMethod("--load-ontology-solr")
	public void loadOntologySolr(Opts opts) throws Exception {
		// Check to see if the global url has been set, otherwise use the local one.
		String url = sortOutSolrURL(opts, globalSolrURL);				

		// Actual ontology class loading.
		try {
			OntologySolrLoader loader = new OntologySolrLoader(url, g);
			loader.load();
		} catch (SolrServerException e) {
			LOG.info("Ontology load at: " + url + " failed!");
			e.printStackTrace();
		}
	}
	
	/**
	 * TODO: Try experimental flexible loader.
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--flex-load-ontology-solr")
	public void flexLoadOntologySolr(Opts opts) throws Exception {
		// Check to see if the global url has been set, otherwise use the local one.
		String url = sortOutSolrURL(opts, globalSolrURL);				

		// Actual ontology class loading.
		try {
			FlexSolrDocumentLoader loader = new FlexSolrDocumentLoader(url, g);
			loader.load();
		} catch (SolrServerException e) {
			LOG.info("Ontology load at: " + url + " failed!");
			e.printStackTrace();
		}

//		// Check to see if the global url has been set, otherwise use the local one.
//		String url = sortOutSolrURL(opts, globalSolrURL);				
//
//		// Load remaining docs.
//		List<String> files = opts.nextList();
//		for (String file : files) {
//			LOG.info("Parsing GAF: " + file);
//			FlexSolrDocumentLoader loader = new FlexSolrDocumentLoader(url);
//			loader.setGafDocument(gafdoc);
//			loader.setGraph(g);
//			try {
//				loader.load();
//			} catch (SolrServerException e) {
//				e.printStackTrace();
//			}
//		}
	}
	
	/**
	 * Used for loading a list of GAFs into GOlr.
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--load-gafs-solr")
	public void loadGafsSolr(Opts opts) throws Exception {
		// Check to see if the global url has been set, otherwise use the local one.
		String url = sortOutSolrURL(opts, globalSolrURL);

		List<String> files = opts.nextList();
		for (String file : files) {
			LOG.info("Parsing GAF: " + file);
			GafObjectsBuilder builder = new GafObjectsBuilder();
			gafdoc = builder.buildDocument(file);
			loadGAFDoc(url, gafdoc);
		}
	}
	
	/**
	 * Requires the --gaf argument (or something else that fills the gafdoc object).
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@CLIMethod("--load-gaf-solr")
	public void loadGafSolr(Opts opts) throws Exception {
		// Double check we're not going to do something silly, like try and
		// use a null variable...
		if( gafdoc == null ){
			System.err.println("No GAF document defined (maybe use '--gaf GAF-FILE') ");
			exit(1);
		}

		// Check to see if the global url has been set, otherwise use the local one.
		String url = null;
		if( globalSolrURL == null ){
			url = opts.nextOpt();
		}else{
			url = globalSolrURL;
		}
		LOG.info("Use GOlr server at: " + url);			

		url = sortOutSolrURL(opts, globalSolrURL);
		// Doc load.
		loadGAFDoc(url, gafdoc);
	}
	
	/*
	 * TODO: Convert all solr URL handling through here.
	 */
	private String sortOutSolrURL(Opts opts, String globalSolrURL){

		String url = null;
		if( globalSolrURL == null ){
			url = opts.nextOpt();
		}else{
			url = globalSolrURL;
		}
		LOG.info("Use GOlr server at: " + url);

		return url;
	}
	
	/*
	 * Wrapper multiple places where there is direct GAF loading.
	 */
	private void loadGAFDoc(String url, GafDocument gafdoc) throws IOException{

		// Doc load.
		GafSolrDocumentLoader loader = new GafSolrDocumentLoader(url);
		loader.setGafDocument(gafdoc);
		loader.setGraph(g);
		try {
			loader.load();
		} catch (SolrServerException e) {
			e.printStackTrace();
		}
	}
}
