package io.github.jpaadmin.metadata;

import io.github.jpaadmin.annotations.ActionScope;

public class ActionMetadata {
    private String id;
    private String name;
    private String icon;
    private String confirmation;
    private ActionScope scope;
    private String methodName;
    private boolean isStatic;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getConfirmation() { return confirmation; }
    public void setConfirmation(String confirmation) { this.confirmation = confirmation; }

    public ActionScope getScope() { return scope; }
    public void setScope(ActionScope scope) { this.scope = scope; }

    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }

    public boolean isStatic() { return isStatic; }
    public void setStatic(boolean aStatic) { isStatic = aStatic; }
}
