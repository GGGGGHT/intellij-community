package com.intellij.psi.impl.source.resolve.reference;

import com.intellij.ant.impl.dom.impl.RegisterInPsi;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiPlainTextFile;
import com.intellij.psi.filters.*;
import com.intellij.psi.filters.position.NamespaceFilter;
import com.intellij.psi.filters.position.ParentElementFilter;
import com.intellij.psi.filters.position.TokenTypeFilter;
import com.intellij.psi.impl.source.jsp.jspJava.JspDirective;
import com.intellij.psi.impl.source.resolve.ResolveUtil;
import com.intellij.psi.impl.source.resolve.reference.impl.manipulators.PlainFileManipulator;
import com.intellij.psi.impl.source.resolve.reference.impl.manipulators.XmlAttributeValueManipulator;
import com.intellij.psi.impl.source.resolve.reference.impl.manipulators.XmlTokenManipulator;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.*;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlToken;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.xml.util.HtmlUtil;
import com.intellij.xml.util.XmlUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: ik
 * Date: 27.03.2003
 * Time: 17:13:45
 * To change this template use Options | File Templates.
 */
public class ReferenceProvidersRegistry implements ProjectComponent {
  private final List<Class> myTempScopes = new ArrayList<Class>();
  private final List<ProviderBinding> myBindings = new ArrayList<ProviderBinding>();
  private final List<Pair<Class, ElementManipulator>> myManipulators = new ArrayList<Pair<Class, ElementManipulator>>();

  public static final ReferenceProvidersRegistry getInstance(Project project) {
    return project.getComponent(ReferenceProvidersRegistry.class);
  }

  private ReferenceProvidersRegistry() {
    // Temp scopes declarations
    myTempScopes.add(PsiIdentifier.class);

    // Manipulators mapping
    registerManipulator(XmlAttributeValue.class, new XmlAttributeValueManipulator());
    registerManipulator(PsiPlainTextFile.class, new PlainFileManipulator());
    registerManipulator(XmlToken.class, new XmlTokenManipulator());
    // Binding declarations
    final JavaClassReferenceProvider classReferenceProvider = new JavaClassReferenceProvider();
    registerReferenceProvider(
      new ScopeFilter(
        new ParentElementFilter(
          new AndFilter(
            new TextFilter(new String[]{"class", "type"}),
            new ParentElementFilter(
              new AndFilter(
                new TextFilter("useBean"),
                new NamespaceFilter(XmlUtil.JSP_URI)
              )
            )
          )
        )),
      XmlAttributeValue.class,
      classReferenceProvider
    );
    RegisterInPsi.referenceProviders(this);

    registerReferenceProvider(
      new ScopeFilter(
        new ParentElementFilter(
          new AndFilter(
            new TextFilter("extends"),
            new ParentElementFilter(
              new AndFilter(
                new OrFilter(
                  new AndFilter(
                    new ClassFilter(XmlTag.class),
                    new TextFilter("directive.page")
                  ),
                  new AndFilter(
                    new ClassFilter(JspDirective.class),
                    new TextFilter("page")
                  )
                ),
                new NamespaceFilter(XmlUtil.JSP_URI)
              )
            )
          )
        )
      ),
      XmlAttributeValue.class, 
      classReferenceProvider
    );

    registerReferenceProvider(
      new ScopeFilter(
        new ParentElementFilter(
          new AndFilter(
            new TextFilter("type"),
            new ParentElementFilter(
              new AndFilter(
                new OrFilter(
                  new AndFilter(
                    new ClassFilter(XmlTag.class),
                    new TextFilter("directive.attribute")
                  ),
                  new AndFilter(
                    new ClassFilter(JspDirective.class),
                    new TextFilter("attribute")
                  )
                ),
                new NamespaceFilter(XmlUtil.JSP_URI)
              )
            )
          )
        )
      ),
      XmlAttributeValue.class, 
      classReferenceProvider
    );
    
    registerReferenceProvider(
      new ScopeFilter(
        new ParentElementFilter(
          new AndFilter(
            new TextFilter("variable-class"),
            new ParentElementFilter(
              new AndFilter(
                new OrFilter(
                  new AndFilter(
                    new ClassFilter(XmlTag.class),
                    new TextFilter("directive.variable")
                  ),
                  new AndFilter(
                    new ClassFilter(JspDirective.class),
                    new TextFilter("variable")
                  )
                ),
                new NamespaceFilter(XmlUtil.JSP_URI)
              )
            )
          )
        )
      ),
      XmlAttributeValue.class, 
      classReferenceProvider
    );
    
    registerReferenceProvider(
      new ScopeFilter(
        new ParentElementFilter(
          new AndFilter(
            new TextFilter("import"),
            new ParentElementFilter(
              new AndFilter(
                new OrFilter(
                  new AndFilter(
                    new ClassFilter(XmlTag.class),
                    new TextFilter("directive.page")
                  ),
                  new AndFilter(
                    new ClassFilter(JspDirective.class),
                    new TextFilter("page")
                  )
                ),
                new NamespaceFilter(XmlUtil.JSP_URI)
              )
            )
          )
        )
      ),
      XmlAttributeValue.class,
      new JspImportListReferenceProvider()
    );

    final JspxIncludePathReferenceProvider pathReferenceProvider = new JspxIncludePathReferenceProvider();
    registerReferenceProvider(
      new ScopeFilter(
        new AndFilter(
          new ParentElementFilter(new TextFilter("errorPage")),
          new ParentElementFilter(
            new AndFilter(
              new NamespaceFilter(XmlUtil.JSP_URI),
              new OrFilter(
                new AndFilter(
                  new ClassFilter(JspDirective.class),
                  new TextFilter("page")
                ),
                new AndFilter(
                  new ClassFilter(XmlTag.class),
                  new TextFilter("directive.page")
                ))
            ), 2
          )
        )
      ),
      XmlAttributeValue.class,
      pathReferenceProvider
    );

    registerReferenceProvider(
      new ScopeFilter(
        new AndFilter(
          new ParentElementFilter(new TextFilter("file")),
          new ParentElementFilter(
            new AndFilter(
              new NamespaceFilter(XmlUtil.JSP_URI),
              new OrFilter(
                new AndFilter(
                  new ClassFilter(JspDirective.class),
                  new TextFilter("include")
                ),
                new AndFilter(
                  new ClassFilter(XmlTag.class),
                  new TextFilter("directive.include")
                ))
            ), 2
          )
        )
      ),
      XmlAttributeValue.class, 
      pathReferenceProvider
    );

    registerReferenceProvider(
      new ScopeFilter(
        new AndFilter(
          new ParentElementFilter(new TextFilter("value")),
          new ParentElementFilter(
            new AndFilter(
              new NamespaceFilter(XmlUtil.JSTL_CORE_URI),
              new AndFilter(
                new ClassFilter(XmlTag.class),
                new TextFilter("url")
              )
            ), 2
          )
        )
      ),
      XmlAttributeValue.class,
      pathReferenceProvider
    );

    registerReferenceProvider(
      new ScopeFilter(
        new AndFilter(
          new ParentElementFilter(new TextFilter("url")),
          new ParentElementFilter(
            new AndFilter(
              new NamespaceFilter(XmlUtil.JSTL_CORE_URI),
              new AndFilter(
                new ClassFilter(XmlTag.class),
                new TextFilter("import")
              )
            ), 2
          )
        )
      ),
      XmlAttributeValue.class,
      pathReferenceProvider
    );

    registerReferenceProvider(
      new ScopeFilter(
        new AndFilter(
          new ParentElementFilter(new TextFilter("page")),
          new ParentElementFilter(
            new AndFilter(
              new NamespaceFilter(XmlUtil.JSP_URI),
              new AndFilter(
                new ClassFilter(XmlTag.class),
                new TextFilter("include")
              )
            ), 2
          )
        )
      ),
      XmlAttributeValue.class, 
      pathReferenceProvider
    );

    registerReferenceProvider(
      new ScopeFilter(
        new AndFilter(
          new ParentElementFilter(new TextFilter("tagdir")),
          new ParentElementFilter(
            new AndFilter(
              new NamespaceFilter(XmlUtil.JSP_URI),
              new AndFilter(
                new ClassFilter(JspDirective.class),
                new TextFilter("taglib")
              )
            ), 2
          )
        )
      ),
      XmlAttributeValue.class, 
      pathReferenceProvider
    );
    
    registerReferenceProvider(
      new ScopeFilter(
        new AndFilter(
          new ParentElementFilter(new TextFilter("uri")),
          new ParentElementFilter(
            new AndFilter(
              new NamespaceFilter(XmlUtil.JSP_URI),
              new AndFilter(
                new ClassFilter(JspDirective.class),
                new TextFilter("taglib")
              )
            ), 2
          )
        )
      ),
      XmlAttributeValue.class, 
      new JspUriReferenceProvider()
    );
    
    //registerReferenceProvider(new ScopeFilter(new ParentElementFilter(new AndFilter(new TextFilter("target"),
    //                                                                                new ParentElementFilter(new AndFilter(
    //                                                                                  new NamespaceFilter(XmlUtil.ANT_URI),
    //                                                                                  new TextFilter("antcall")))))),
    //                          XmlAttributeValue.class, new AntTargetReferenceProvider());
    registerReferenceProvider(new NotFilter(new ParentElementFilter(new NamespaceFilter(XmlUtil.ANT_URI), 2)),
                              XmlAttributeValue.class, new JavaClassListReferenceProvider());
    registerReferenceProvider(new TokenTypeFilter(XmlTokenType.XML_DATA_CHARACTERS), XmlToken.class,
                              new JavaClassListReferenceProvider());

    //registerReferenceProvider(PsiPlainTextFile.class, new JavaClassListReferenceProvider());

    HtmlUtil.HtmlReferenceProvider provider = new HtmlUtil.HtmlReferenceProvider();
    registerReferenceProvider(provider.getFilter(), XmlAttributeValue.class, provider);

    final PsiReferenceProvider filePathReferenceProvider = new FilePathReferenceProvider();
    registerReferenceProvider(PsiLiteralExpression.class, filePathReferenceProvider);
  }

  public void registerReferenceProvider(ElementFilter elementFilter, Class scope, PsiReferenceProvider provider) {
    final ProviderBinding binding = new ProviderBinding(elementFilter, scope);
    binding.registerProvider(provider);
    myBindings.add(binding);
  }

  public void registerReferenceProvider(Class scope, PsiReferenceProvider provider) {
    final ProviderBinding binding = new ProviderBinding(scope);
    binding.registerProvider(provider);
    myBindings.add(binding);
  }

  public PsiReferenceProvider[] getProvidersByElement(PsiElement element) {
    final List<PsiReferenceProvider> ret = new ArrayList<PsiReferenceProvider>();
    PsiElement current;
    do {
      current = element;

      for (final ProviderBinding binding : myBindings) {
        if (binding.isAcceptable(current)) {
          ret.addAll(Arrays.asList(binding.getProviders()));
        }
      }
      element = ResolveUtil.getContext(element);
    }
    while (!isScopeFinal(current.getClass()));

    return ret.toArray(new PsiReferenceProvider[ret.size()]);
  }

  public <T extends PsiElement> ElementManipulator<T> getManipulator(T element) {
    if(element == null) return null;

    for (final Pair<Class, ElementManipulator> pair : myManipulators) {
      if (pair.getFirst().isAssignableFrom(element.getClass())) {
        return (ElementManipulator<T>)pair.getSecond();
      }
    }

    return null;
  }

  public <T extends PsiElement> void registerManipulator(Class<T> elementClass, ElementManipulator<T> manipulator) {
    myManipulators.add(new Pair<Class, ElementManipulator>(elementClass, manipulator));
  }

  private boolean isScopeFinal(Class scopeClass) {

    for (final Class aClass : myTempScopes) {
      if (aClass.isAssignableFrom(scopeClass)) {
        return false;
      }
    }
    return true;
  }

  public void projectOpened() {}

  public void projectClosed() {}

  public String getComponentName() {
    return "Reference providers registry";
  }

  public void initComponent() {}

  public void disposeComponent() {}
}
