package org.eclipse.thingweb.directory.servlet;

import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.thingweb.directory.ThingDirectory;
import org.eclipse.thingweb.directory.rest.RESTServlet;
import org.eclipse.thingweb.directory.servlet.utils.BufferedResponseWrapper;
import org.eclipse.thingweb.directory.servlet.utils.RedirectedRequestWrapper;
import org.eclipse.thingweb.directory.sparql.client.Connector;
import org.eclipse.thingweb.directory.util.Frame2SPARQL;
import org.eclipse.thingweb.directory.utils.TDTransform;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

/**
 * org.eclipse.thingweb.directory.servlet
 * <p>
 * TODO: Add class description
 * <p>
 * Author:  Anh Le-Tuan
 * <p>
 * Email:   anh.letuan@insight-centre.org
 * <p>
 * Date:  25/06/18.
 */
public class TDFramingServlet extends RESTServlet {
  private static final String[] ACCEPTED = { "application/json" };
  private static final String QUERY_PARAMETER = "frame";

  protected final TDServlet tdServlet;

  public TDFramingServlet(TDServlet tdServlet){
    this.tdServlet = tdServlet;
  }

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    super.doGet(req, resp);

    if (resp.isCommitted()) {
      return; // parent class returned an error status
    }

    String frame = req.getParameter(QUERY_PARAMETER);
    Frame2SPARQL frame2SPARQL = new Frame2SPARQL();
    RepositoryConnection connection = Connector.getRepositoryConnection();

    String td;
    try{
      Object framed = frame2SPARQL.frame_(connection, frame);
      td     = JsonUtils.toPrettyString(framed);
    } catch (IllegalArgumentException e){
      td = e.getMessage();
    }


    BufferedResponseWrapper respWrapper = new BufferedResponseWrapper(resp);

    if (respWrapper.getStatus() < HttpServletResponse.SC_BAD_REQUEST) {
      OutputStream outputStream = resp.getOutputStream();
      outputStream.write(td.getBytes());
      outputStream.close();
    }
  }

  @Override
  protected String[] getAcceptedContentTypes() {
    return ACCEPTED;
  }
}
