package doext.define;

import org.json.JSONObject;

import core.interfaces.DoIPage;
import core.interfaces.DoIScriptEngine;
import core.object.DoInvokeResult;

/**
 * 声明自定义扩展组件方法
 */
public interface do_Page_IMethod extends DoIPage {
	void getData(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception;

	void hideKeyboard(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception;

	void remove(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception;

//	void supportPanClosePage(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception;
}