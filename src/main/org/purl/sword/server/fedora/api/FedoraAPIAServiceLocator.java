/**
 * FedoraAPIAServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.purl.sword.server.fedora.api;

public class FedoraAPIAServiceLocator extends org.apache.axis.client.Service implements org.purl.sword.server.fedora.api.FedoraAPIAService {

    public FedoraAPIAServiceLocator() {
    }


    public FedoraAPIAServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public FedoraAPIAServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for access
    private java.lang.String access_address = "http://localhost:8080/fedora/services/access";

    public java.lang.String getaccessAddress() {
        return access_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String accessWSDDServiceName = "access";

    public java.lang.String getaccessWSDDServiceName() {
        return accessWSDDServiceName;
    }

    public void setaccessWSDDServiceName(java.lang.String name) {
        accessWSDDServiceName = name;
    }

    public org.purl.sword.server.fedora.api.FedoraAPIA getaccess() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(access_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getaccess(endpoint);
    }

    public org.purl.sword.server.fedora.api.FedoraAPIA getaccess(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            org.purl.sword.server.fedora.api.AccessSoapBindingStub _stub = new org.purl.sword.server.fedora.api.AccessSoapBindingStub(portAddress, this);
            _stub.setPortName(getaccessWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setaccessEndpointAddress(java.lang.String address) {
        access_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (org.purl.sword.server.fedora.api.FedoraAPIA.class.isAssignableFrom(serviceEndpointInterface)) {
                org.purl.sword.server.fedora.api.AccessSoapBindingStub _stub = new org.purl.sword.server.fedora.api.AccessSoapBindingStub(new java.net.URL(access_address), this);
                _stub.setPortName(getaccessWSDDServiceName());
                return _stub;
            }
        }
        catch (java.lang.Throwable t) {
            throw new javax.xml.rpc.ServiceException(t);
        }
        throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        java.lang.String inputPortName = portName.getLocalPart();
        if ("access".equals(inputPortName)) {
            return getaccess();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://www.fedora.info/definitions/1/0/api/", "Fedora-API-A-Service");
    }

    private java.util.HashSet ports = null;

	 @SuppressWarnings(value={"unchecked"})
    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://www.fedora.info/definitions/1/0/api/", "access"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("access".equals(portName)) {
            setaccessEndpointAddress(address);
        }
        else 
{ // Unknown Port Name
            throw new javax.xml.rpc.ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
        }
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(javax.xml.namespace.QName portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        setEndpointAddress(portName.getLocalPart(), address);
    }
protected org.apache.axis.EngineConfiguration getEngineConfiguration() {
System.out.println("Calling engine config");
    java.lang.StringBuffer sb = new java.lang.StringBuffer();
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
    sb.append("<deployment name=\"defaultClientConfig\"\r\n");
    sb.append("xmlns=\"http://xml.apache.org/axis/wsdd/\"\r\n");
    sb.append("xmlns:java=\"http://xml.apache.org/axis/wsdd/providers/java\">\r\n");
    sb.append("<transport name=\"http\" pivot=\"java:org.apache.axis.transport.http.CommonsHTTPSender\" />\r\n");
    sb.append("<transport name=\"local\" pivot=\"java:org.apache.axis.transport.local.LocalSender\" />\r\n");
    sb.append("<transport name=\"java\" pivot=\"java:org.apache.axis.transport.java.JavaSender\" />\r\n");
    sb.append("</deployment>\r\n");
    org.apache.axis.configuration.XMLStringProvider config = 
        new org.apache.axis.configuration.XMLStringProvider(sb.toString());
    return config;
}

}
