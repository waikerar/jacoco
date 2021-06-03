/*******************************************************************************
 * Copyright (c) 2009, 2021 Mountainminds GmbH & Co. KG and Contributors
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Arvind Waiker - initial API and implementation
 *
 *******************************************************************************/
package org.jacoco.core.internal.analysis;

import org.jacoco.core.analysis.IMethodCoverage;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

/**
 * Scala Class Analyzer. Java Version of the filter logic from
 * [[https://github.com/sbt/sbt-jacoco]]
 */
public class ScalaClassAnalyzer extends ClassAnalyzer {
	/**
	 * Creates a new analyzer that builds coverage data for a class.
	 *
	 * @param coverage
	 *            coverage node for the analyzed class data
	 * @param probes
	 *            execution data for this class or <code>null</code>
	 * @param stringPool
	 *            shared pool to minimize the number of {@link String} instances
	 */
	public ScalaClassAnalyzer(ClassCoverageImpl coverage, boolean[] probes,
			StringPool stringPool) {
		super(coverage, probes, stringPool);
	}

	@Override
	protected boolean ignore(MethodNode node, IMethodCoverage mc,
			ClassCoverageImpl coverage) {
		boolean isScala = getClassAttributes().contains("ScalaSig")
				|| getClassAttributes().contains("Scala");

		boolean isModuleStaticInt = coverage.getName().endsWith("$")
				&& node.name.equals("<clint>");

		if (isScala) {
			return isSyntheticMethod(coverage.getName(), node.name,
					mc.getFirstLine(), mc.getLastLine()) || isModuleStaticInt
					|| isScalaForwarder(coverage.getName(), node)
					|| isAccessor(node);
		}
		return false;
	}

	protected Boolean isSyntheticMethod(String owner, String name,
			int firstLine, int lastLine) {
		boolean isModuleClass = owner.endsWith("$");
		boolean isOneLiner = firstLine == lastLine;
		return isOneLiner && ((isModuleClass && (isCaseCompanionMethod(name)
				|| isAnyValCompanionMethod(name)))
				|| isCaseInstanceMethod(name));
	}

	private Boolean isCaseInstanceMethod(String name) {
		return "canEqual".equals(name) || "copy".equals(name)
				|| "equals".equals(name) || "hashCode".equals(name)
				|| "productPrefix".equals(name) || "productArity".equals(name)
				|| "productElement".equals(name)
				|| "productElementNames".equals(name)
				|| "productElementName".equals(name)
				|| "productIterator".equals(name) || "toString".equals(name)
				|| name.startsWith("copy$default");
	}

	private Boolean isCaseCompanionMethod(String name) {
		return "apply".equals(name) || "unapply".equals(name)
				|| "unapplySeq".equals(name) || "readResolve".equals(name);
	}

	private Boolean isAnyValCompanionMethod(String name) {
		return "equals$extension".equals(name)
				|| "hashCode$extension".equals(name);
	}

	private Boolean isAccessor(MethodNode node) {
		if (node.instructions.size() < 10) {
			Iterator<AbstractInsnNode> instList = node.instructions.iterator();
			List<AbstractInsnNode> filteredList = new ArrayList<AbstractInsnNode>();
			while (instList.hasNext()) {
				AbstractInsnNode inst = instList.next();
				if (!(inst instanceof LabelNode
						| inst instanceof LineNumberNode)) {
					filteredList.add(inst);
				}
			}

			if (filteredList.size() < 3) {
				return false;
			}

			if (filteredList.get(0).getOpcode() != ALOAD
					|| filteredList.get(1).getOpcode() != GETFIELD) {
				return false;
			}

			int returnCode = filteredList.get(2).getOpcode();

			return returnCode == IRETURN || returnCode == LRETURN
					|| returnCode == FRETURN || returnCode == DRETURN
					|| returnCode == ARETURN || returnCode == RETURN;
		}
		return false;
	}

	private Boolean isScalaForwarder(String className, MethodNode node) {
		if (node.instructions.size() <= 100) {
			Iterator<AbstractInsnNode> instList = node.instructions.iterator();
			boolean hasJump = false;
			while (instList.hasNext()) {
				AbstractInsnNode inst = instList.next();
				if (inst instanceof JumpInsnNode) {
					hasJump = true;
					break;
				}
			}

			instList = node.instructions.iterator();
			while (instList.hasNext()) {
				AbstractInsnNode inst = instList.next();
				if (inst instanceof MethodInsnNode) {
					MethodInsnNode methodInsnNode = (MethodInsnNode) inst;
					return isScalaForwarder(className, node.name,
							methodInsnNode.getOpcode(), methodInsnNode.owner,
							methodInsnNode.name, hasJump);
				}
			}
		}
		return false;
	}

	private Boolean isScalaForwarder(String className, String methodName,
			int opCode, String calledMethodOwner, String calledMethodName,
			boolean hasJump) {
		boolean callingCompanionModule = calledMethodOwner
				.equals(className + "$");
		boolean callingImplClass = calledMethodOwner.endsWith("$class");
		boolean callingImplicitClass = calledMethodOwner.endsWith(
				"$" + methodName) || calledMethodOwner.equals(methodName);
		String extensionName = methodName + "$extension";

		boolean staticForwarder = opCode == INVOKEVIRTUAL
				&& callingCompanionModule
				&& calledMethodName.equals(methodName);
		boolean traitForwarder = opCode == INVOKESTATIC && callingImplClass
				&& calledMethodName.equals(methodName);
		boolean extensionMethodForwarder = opCode == INVOKEVIRTUAL
				&& callingCompanionModule
				&& calledMethodName.equals(extensionName);
		boolean implicitClassFactory = opCode == INVOKESPECIAL
				&& callingImplicitClass && calledMethodName.equals("<init>");
		boolean lazyAccessor = opCode == INVOKESPECIAL
				&& calledMethodName.endsWith("$lzycompute");
		return ((staticForwarder || traitForwarder || extensionMethodForwarder
				|| implicitClassFactory) && !hasJump || lazyAccessor);
	}
}
