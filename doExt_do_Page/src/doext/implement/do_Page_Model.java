package doext.implement;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import core.DoServiceContainer;
import core.helper.DoJsonHelper;
import core.helper.DoScriptEngineHelper;
import core.helper.DoUIModuleHelper;
import core.interfaces.DoIApp;
import core.interfaces.DoIPageView;
import core.interfaces.DoIScriptEngine;
import core.object.DoInvokeResult;
import core.object.DoMultitonModule;
import core.object.DoSingletonModule;
import core.object.DoSourceFile;
import core.object.DoUIContainer;
import core.object.DoUIModule;
import doext.define.do_Page_IMethod;

/**
 * 自定义扩展SM组件Model实现，继承DoSingletonModule抽象类，并实现do_Page_IMethod接口方法；
 * #如何调用组件自定义事件？可以通过如下方法触发事件：
 * this.model.getEventCenter().fireEvent(_messageName, jsonResult);
 * 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象； 获取DoInvokeResult对象方式new
 * DoInvokeResult(this.getUniqueKey());
 */
public class do_Page_Model extends DoSingletonModule implements do_Page_IMethod {

	private DoIApp currentApp;
	private DoIPageView pageView;
	private DoSourceFile uiFile;
	private DoIScriptEngine scriptEngine;
	private DoUIModule rootView;
	private String pageData;
	private Map<String, DoUIModule> dictUIModuleAddresses;
	private boolean isFullScreen;
	private int designScreenWidth;
	private int designScreenHeight;

	public DoIApp getCurrentApp() {
		return currentApp;
	}

	public DoIPageView getPageView() {
		return pageView;
	}

	@Override
	public DoSourceFile getUIFile() {
		return uiFile;
	}

	public DoIScriptEngine getScriptEngine() {
		return scriptEngine;
	}

	@Override
	public DoUIModule getRootView() {
		return this.rootView;
	}

	@Override
	public String getData() {
		return this.pageData;
	}

	@Override
	public void setData(String data) {
		this.pageData = data;
	}

	public do_Page_Model(DoIApp _doApp, DoIPageView _pageView, DoSourceFile _uiFile) throws Exception {
		super();
		this.currentApp = _doApp;
		this.pageView = _pageView;
		this.uiFile = _uiFile;
		this.designScreenWidth = DoServiceContainer.getGlobal().getDesignScreenWidth();
		this.designScreenHeight = DoServiceContainer.getGlobal().getDesignScreenHeight();
	}

	@Override
	public void onInit() throws Exception {
		super.onInit();

		// 初始化成员变量
		this.dictUIModuleAddresses = new HashMap<String, DoUIModule>();
		this.dictMultitonModuleIDs = new HashMap<String, String>();
		this.dictMultitonModuleAddresses = new HashMap<String, DoMultitonModule>();
	}

	@Override
	public void dispose() {
		DoServiceContainer.getSingletonModuleFactory().removeSingletonModuleByAddress(this.getUniqueKey());
		if (this.dictUIModuleAddresses != null) {
			this.dictUIModuleAddresses.clear();
			this.dictUIModuleAddresses = null;
		}

		if (this.scriptEngine != null) {
			this.scriptEngine.dispose();
			this.scriptEngine = null;
		}
		if (this.dictMultitonModuleIDs != null) {
			this.dictMultitonModuleIDs.clear();
			this.dictMultitonModuleIDs = null;
		}

		if (this.dictMultitonModuleAddresses != null) {
			// 释放每一个子Model
			for (DoMultitonModule _multitonModule : this.dictMultitonModuleAddresses.values()) {
				_multitonModule.dispose();
			}
			this.dictMultitonModuleAddresses.clear();
			this.dictMultitonModuleAddresses = null;
		}

		if (this.rootView != null) {
			this.rootView.dispose();
			this.rootView = null;
		}
		super.dispose();
	}

	@Override
	public DoUIModule createUIModule(DoUIContainer _uiContainer, JSONObject _moduleNode) throws Exception {
		String _typeID = DoJsonHelper.getString(_moduleNode, "type", "");
		DoUIModule _uiModule = DoServiceContainer.getUIModuleFactory().createUIModule(_typeID);
		if (_uiModule == null)
			throw new Exception(this.getUIFile().getFileFullName() + "中遇到无效的UI组件：" + _typeID);
		_uiModule.setCurrentPage(this);
		_uiModule.setCurrentContainer(_uiContainer);
		_uiModule.loadModel(_moduleNode);
		DoServiceContainer.getUIModuleFactory().bindUIModuleView(_uiModule);
		this.dictUIModuleAddresses.put(_uiModule.getUniqueKey(), _uiModule);
		_uiModule.loadView();
		_uiContainer.registChildUIModule(_uiModule.getID(), _uiModule);
		return _uiModule;
	}

	@Override
	public void loadRootUiContainer() throws Exception {
		DoUIContainer _rootUIContainer = new DoUIContainer(this);
		_rootUIContainer.loadFromFile(this.uiFile, null, null);
		this.rootView = _rootUIContainer.getRootView();
	}

	@Override
	public void loadScriptEngine(String _scriptFile, String _scriptType, String _fileName) throws Exception {
		String _type = DoServiceContainer.getGlobal().getScriptType();
		if (!TextUtils.isEmpty(_scriptType)) {
			_type = "." + _scriptType;
		}
		String _scriptFileName = _fileName.substring(_fileName.lastIndexOf("/") + 1, _fileName.length()) + _type;
		this.scriptEngine = DoServiceContainer.getScriptEngineFactory().createScriptEngine(this.currentApp, this, _scriptType, _scriptFileName);

		if (this.scriptEngine == null)
			throw new Exception(this.getUIFile().getFileFullName() + "中的脚本类型无效：" + _type);

		if (_scriptFile != null && _scriptFile.length() > 0) {
			if (this.rootView != null) {
				this.rootView.getUIContainer().loadDefalutScriptFile(_scriptFile, _type);
			}
		}
	}

	@Override
	public DoUIModule getUIModuleByAddress(String _key) {
		if (!this.dictUIModuleAddresses.containsKey(_key))
			return null;
		return this.dictUIModuleAddresses.get(_key);
	}

	@Override
	public void removeUIModule(DoUIModule _uiModule) {
		if (this.dictUIModuleAddresses != null && this.dictUIModuleAddresses.size() > 0) {
			this.dictUIModuleAddresses.remove(_uiModule);
		}
	}

	private Map<String, DoMultitonModule> dictMultitonModuleAddresses;

	@Override
	public DoMultitonModule createMultitonModule(String _typeID, String _id) throws Exception {
		if (_typeID == null || _typeID.length() <= 0)
			throw new Exception("未指定Model组件的type值");
		DoMultitonModule _multitonModule = null;
		if (_id != null && _id.length() > 0) {
			String _tempID = _typeID + _id;
			String _address = this.dictMultitonModuleIDs.get(_tempID);
			if (_address != null) {
				_multitonModule = this.dictMultitonModuleAddresses.get(_address);
			}
		}

		if (_multitonModule == null) {
			_multitonModule = DoServiceContainer.getMultitonModuleFactory().createMultitonModule(_typeID);
			if (_multitonModule == null)
				throw new Exception("遇到无效的Model组件：" + _typeID);
			_multitonModule.setCurrentPage(this);
			_multitonModule.setCurrentApp(this.currentApp);
			this.dictMultitonModuleAddresses.put(_multitonModule.getUniqueKey(), _multitonModule);
			if (_id != null && _id.length() > 0) {
				String _tempID = _typeID + _id;
				this.dictMultitonModuleIDs.put(_tempID, _multitonModule.getUniqueKey());
			}
		}
		return _multitonModule;
	}

	@Override
	public boolean deleteMultitonModule(String _address) {
		DoMultitonModule _multitonModule = this.getMultitonModuleByAddress(_address);
		if (_multitonModule == null)
			return false;
		_multitonModule.dispose();
		this.dictMultitonModuleAddresses.remove(_address);
		for (String key : this.dictMultitonModuleIDs.keySet()) {
			if (_address.equalsIgnoreCase(this.dictMultitonModuleIDs.get(key))) {
				this.dictMultitonModuleIDs.remove(key);
				break;
			}
		}
		return true;
	}

	@Override
	public DoMultitonModule getMultitonModuleByAddress(String _key) {
		if (!this.dictMultitonModuleAddresses.containsKey(_key))
			return null;
		return this.dictMultitonModuleAddresses.get(_key);
	}

	// 处理成员方法
	@Override
	public boolean invokeSyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		if ("getData".equals(_methodName)) { // 获取从上一层page传递过来的数据
			this.getData(_dictParas, _scriptEngine, _invokeResult);
			return true;
		} else if ("remove".equals(_methodName)) { // 根据id或者地址移除view
			this.remove(_dictParas, _scriptEngine, _invokeResult);
			return true;
		} else if ("hideKeyboard".equals(_methodName)) { // 隐藏软键盘
			this.hideKeyboard(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		return super.invokeSyncMethod(_methodName, _dictParas, _scriptEngine, _invokeResult);
	}

	@Override
	public boolean invokeAsyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		return super.invokeAsyncMethod(_methodName, _dictParas, _scriptEngine, _callbackFuncName);
	}

	@Override
	public void getData(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) {
		_invokeResult.setResultText(this.getData());
	}

	private Map<String, String> dictMultitonModuleIDs;

	@Override
	public void remove(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		String _id = DoJsonHelper.getString(_dictParas, "id", "");
		DoUIModule _uiModule = null;
		if (_id.length() == 0) {
			new Exception("id不能为空");
		} else {
			_uiModule = DoScriptEngineHelper.parseUIModule(_scriptEngine, _id);
		}
		if (_uiModule != null) {
			View _removeView = (View) _uiModule.getCurrentUIModuleView();
			if (_removeView != null) {
				DoUIModuleHelper.hideKeyboard(_removeView);
				DoUIModuleHelper.removeFromSuperview(_removeView);
				_uiModule.dispose();
			}
		}
	}

	@Override
	public void hideKeyboard(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		Activity _activity = DoServiceContainer.getPageViewFactory().getAppContext();
		InputMethodManager _imm = ((InputMethodManager) _activity.getSystemService(Context.INPUT_METHOD_SERVICE));
		View _focusView = _activity.getCurrentFocus();
		if (_focusView != null) {
			_focusView.setFocusable(false);
			_focusView.clearFocus();
			_imm.hideSoftInputFromWindow(_focusView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
			_activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		}
	}

	@Override
	public void fireEvent(String _eventName, Object _result) {
		DoInvokeResult _invokeResult = new DoInvokeResult(this.getUniqueKey());
		if (_result != null) {
			if (_result instanceof JSONObject) {
				_invokeResult.setResultNode((JSONObject) _result);
			} else {
				_invokeResult.setResultText(_result.toString());
			}
		}
		this.getEventCenter().fireEvent(_eventName, _invokeResult);
	}

	@Override
	public void setFullScreen(boolean isFullScreen) {
		this.isFullScreen = isFullScreen;
	}

	@Override
	public boolean isFullScreen() {
		return isFullScreen;
	}

	private boolean isTransparent;

	@Override
	public void setTransparent(boolean transparent) {
		this.isTransparent = transparent;
	}

	@Override
	public boolean isTransparent() {
		return isTransparent;
	}

	@Override
	public void setDesignScreenResolution(int screenWidth, int screenHeight) {
		this.designScreenWidth = screenWidth;
		this.designScreenHeight = screenHeight;
	}

	@Override
	public int getDesignScreenWidth() {
		return designScreenWidth;
	}

	@Override
	public int getDesignScreenHeight() {
		return designScreenHeight;
	}
}