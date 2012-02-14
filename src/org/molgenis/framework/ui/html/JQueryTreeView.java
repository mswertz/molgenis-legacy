package org.molgenis.framework.ui.html;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.molgenis.util.SimpleTree;

public class JQueryTreeView<E> extends HtmlWidget
{
	private SimpleTree<JQueryTreeViewElement> treeData;
	
	private List<String> listOfMeasurements = new ArrayList<String>();
			
	public JQueryTreeView(String name, SimpleTree<JQueryTreeViewElement> treeData)
	{
		super(name);
		this.treeData = treeData;
	}
	
	/**
	 * Checks whether the given node contains labels that have been selected/toggled,
	 * entailing the node should be presented expanded (opened).
	 * 
	 * @param node
	 * @param selected
	 * @return
	 */
	private boolean nodeOpen(JQueryTreeViewElement node, List<String> selectedLabels) {
		Vector<JQueryTreeViewElement> children = node.getChildren();
		// iterate through all children of this node
		for (JQueryTreeViewElement child : children) {
			// if this child is in the selected list, node should be open
			if (selectedLabels.contains(child.getLabel())) {
				return true;
			}
			// if this child has children and one of them is in the selected list, node should be open
			if (child.hasChildren()) {
				if (nodeOpen(child, selectedLabels) == true) {
					return true;
				}
			}
		}
		// no (grand)child selected, so node should be closed
		return false;
	}
	
	/**
	 * 
	 * No Nodes collapsed explicitly. Node is manually closed then by giving its LI element a "closed"(/opened) CSS class.
	 * Animation enabled, speed is "normal".
	 * "Cookie" persistence enabled, causing the current tree state to be persisted.
	 *	Dynamically adding a sub tree to the existing tree demonstrated.
	 */
	private String renderTree(JQueryTreeViewElement node, List<String> selectedLabels) {
		String returnString;
		if (node.hasChildren()) {
			returnString = "<li class=\"" + (nodeOpen(node, selectedLabels) ? "opened" : "closed") + 
					"\"><span class=\"folder\">" + node.getLabel() + "</span>";
			//returnString += " <b>details: </b>"+ node;
			returnString += "<ul>";
			Vector<JQueryTreeViewElement> children = node.getChildren();
			for (JQueryTreeViewElement child : children) {
				returnString += renderTree(child, selectedLabels);
			}
			returnString += "</ul></li>";
		} else {
			returnString = "<li id = \"" + node.getLabel().replaceAll(" ", "_") + "\"><span class=\"point\"><input type=\"checkbox\" id=\"" + 
				node.getLabel() + "\" name=\"" + node.getLabel() + "\"" + 
				(selectedLabels.contains(node.getLabel()) ? " checked=\"yes\"" : "") + 
				" />" + node.getLabel() + "</span></li>" +
				"<script>createHashMap(\"" + node.getLabel() + "\",\"" + node.getHtmlValue() + "\")</script>";
				
			listOfMeasurements.add(node.getLabel());
		}
		return returnString;
	}
	
	public String toHtml(List<String> selected){
		
		String html = "<script>var map = new HashMap();</script><div id=\"masstoggler\">	<a title=\"Collapse entire tree\" href=\"#\">Collapse all</a> | ";
		html += "<a title=\"Expand entire tree\" href=\"#\">Expand all</a>" ;
		//html += " | <a title=\"Toggle the tree below\" href=\"#\">Toggle all</a></div> ";
		html += "<ul id=\"browser\" class=\"pointtree\">";
		html += renderTree(treeData.getRoot(), selected);
		html += "</ul>";
		html += "<script src=\"res/jquery-plugins/datatables/js/jquery.js\"></script>\n"
		+ "<link rel=\"stylesheet\" href=\"res/jquery-plugins/Treeview/jquery.treeview.css\" type=\"text/css\" media=\"screen\" />\n" 
		+ "<script src=\"res/jquery-plugins/Treeview/jquery.treeview.js\" language=\"javascript\"></script>"
		+ " <style type=\"text/css\">\n"
		+ " li {cursor:pointer; width:200px;}\n"
		+ " li.highlight { background:red; }\n"
		+ "#browser {\n"
		+ "font-family: Verdana, helvetica, arial, sans-serif;\n"
		+ "font-size: 68.75%;\n"
		+"}\n"
		+ "#details{position:absolute;right:100px;top:200px;}\n"
		+ "#leftSideTree{width:300px;}\n"
		+"</style>\n"
		+"<script>\n"
		+ "$(\"li > span\").hover(function () {"
        + "$(this).addClass(\"highlight\");"
        + "}, function () {"
        + "$(this).removeClass(\"highlight\");"
        + "});"
		+"$(document).ready(function(){\n"
		+"$(\"#browser\").treeview({control: \"#masstoggler\"});" 
		+"});\n"
//		+ "$(document).unload(function() {"
//		+ "alert('Handler for .unload() called.');"
//		+ "});"
		+ "</script>\n";
	    
	    String measurementClickEvent = "<script>";
	    
	    for(String eachMeasurement : listOfMeasurements){
	    	measurementClickEvent += "$('#" + eachMeasurement.replaceAll(" ", "_") + "').click(function() {"
						    			+ "getHashMapContent(\"" + eachMeasurement + "\");});"
						    			+ "";
	    }
	    measurementClickEvent += "</script>";
	    
	    html += measurementClickEvent;
	    
	    return html;
	}
	
	@Override
	public String toHtml(){
		return "";
	}
}

