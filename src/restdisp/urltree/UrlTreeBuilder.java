package restdisp.urltree;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import restdisp.io.IOUtil;
import restdisp.validation.ConfigParser;
import restdisp.validation.ConfigurationException;

public class UrlTreeBuilder {
	public static Node buildUrlTree(InputStream is) throws ConfigurationException {
		String confString = null;
		try {
			confString = IOUtil.toString(is);
		} catch (IOException e) {
			throw new ConfigurationException("Could not read stream", e);
		}
		String[] strEntries = ConfigParser.parse(confString);

		Node root = new Node("root", new ArrayList<Node>(), null, false);
		for (String entry : strEntries) {
			if (entry.length() > 0 && entry.charAt(0) != '#') {
				addEntryToTree(root, entry);
			}
		}
		return root;
	}
	
	private static void addEntryToTree(Node root, String entry) throws ConfigurationException {
		String[] items = entry.split(" ");
		if (items.length != 3) {
			throw new ConfigurationException(String.format("Wrong configuration entry [%s]", entry));
		}
		
		items[1] = String.format("/%s%s", items[0], items[1]);
		String[] nodes = items[1].replaceFirst("/", "").split("(?<=\\w|})/");
		try {
			addBranchToTree(root, nodes, items[2]);
		} catch (ConfigurationException e) {
			throw new ConfigurationException(String.format("Failed to add branch [%s]", items[1]), e);
		}
	}

	private static void addBranchToTree(Node root, String[] nodes, String classAndMethod) throws ConfigurationException {
		String[] classAndMethodArr = classAndMethod.split("\\:");
		List<Node> children = root.getChildren();
		i: for (int i = 0; i < nodes.length; i++) {
			Node tmpNode = new Node(getVarname(nodes[i]));
			
			if (!children.contains(tmpNode)) {
				children.add(tmpNode);
				tmpNode.setVar(isVar(nodes[i]));
				children = new ArrayList<Node>();
				tmpNode.setChildren(children);
				if (i + 1 == nodes.length){
					tmpNode.setLeaf(buildLeaf(classAndMethodArr, nodes));
				}
			} else {
				for (Node node : children) {
					if (node.equals(tmpNode)) {
						if (i + 1 == nodes.length && node.getLeaf() == null) {
							node.setLeaf(buildLeaf(classAndMethodArr, nodes));
						}
						children = node.getChildren();
						continue i;
					}
				}
			}
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Leaf buildLeaf(String[] classAndMethodArr, String[] nodes) throws ConfigurationException {
		try {
			Class cls = Class.forName(classAndMethodArr[0]);
			Constructor constructor = cls.getConstructor();
			int varCnt = getVariableCount(nodes);
			Method meth = getMethod(cls, classAndMethodArr[1], varCnt);
			return new Leaf(cls, constructor, meth);
		} catch (ClassNotFoundException e) {
			throw new ConfigurationException(String.format("Failed to build leaf. Class not found [%s].", classAndMethodArr[0]), e);
		} catch (NoSuchMethodException e) {
			throw new ConfigurationException(String.format("Failed to build leaf. Method not found [%s:%s].", classAndMethodArr[0], classAndMethodArr[1]), e);
		}
	}
	
	private static int getVariableCount(String[] nodes) {
		int cnt = 0;
		for (String str : nodes) {
			if (str.charAt(0) == '{'){
				cnt++;
			}
		}
		return cnt;
	}
	
	@SuppressWarnings("rawtypes") 
	private static Method getMethod(Class cls, String methodName, int varCnt) throws ConfigurationException {
		Method meth = null;
		Method[] mths = cls.getMethods();
		for (Method curMet : mths) {
			if (curMet.getName().equals(methodName) && curMet.getParameterTypes().length == varCnt) {
				meth = curMet;
			}
		}
		
		if (null == meth) {
			throw new ConfigurationException(String.format("Method not found [%s:%s]. Variables count [%s].", cls.toString(), methodName, varCnt));
		}
		return meth;
	}

	private static boolean isVar(String url) {
		return url.contains("{") || url.contains("}"); 
	}
	
	private static String getVarname(String url) {
		return url.replaceAll("\\{|\\}", "");
	}
}
