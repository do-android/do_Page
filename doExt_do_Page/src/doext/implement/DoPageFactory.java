package doext.implement;

import core.interfaces.DoIApp;
import core.interfaces.DoIPage;
import core.interfaces.DoIPageFactory;
import core.interfaces.DoIPageView;
import core.object.DoSourceFile;

public class DoPageFactory implements DoIPageFactory {

	@Override
	public DoIPage createPage(DoIApp _doApp, DoIPageView _pageView, DoSourceFile _uiFile) throws Exception {
		return new do_Page_Model(_doApp, _pageView, _uiFile);
	}
}
