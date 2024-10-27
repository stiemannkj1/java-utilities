package dev.stiemannkj1.bytecode.namespacer;

import dev.stiemannkj1.bytecode.namespacer.NamespacerMainTests.MyService;

public final class KeepNamespaceTestServiceImpl implements KeepNamespaceTestService, MyService {

  public KeepNamespaceTestServiceImpl() {}

  @Override
  public String getString() {
    return "dev.stiemannkj1.bytecode.namespacer.KeepNamespaceTestServiceImpl";
  }

  @Override
  public String toString() {
    return getString();
  }
}
