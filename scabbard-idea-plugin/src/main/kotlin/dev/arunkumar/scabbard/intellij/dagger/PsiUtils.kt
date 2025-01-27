package dev.arunkumar.scabbard.intellij.dagger

import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject

private val DaggerComponentAnnotations = listOf(
  DAGGER_COMPONENT,
  DAGGER_SUBCOMPONENT,
  DAGGER_MODULE
)

/**
 * @return `true` only when
 * 1. The given PSI element is an `PsiIdentifier` in `Java` language.
 * 2. and is a dagger annotation entry (present in `daggerAnnotations`).
 * For example, for `@Component` annotation on a class or an interface, this method will return `true` only for
 * `Component` part when it is represented as a `PsiIdentifier`.
 *
 * @param daggerAnnotations the list of dagger annotation to look for.
 * @see DaggerComponentAnnotations
 */
fun PsiElement.isDaggerAnnotationIdentifier(daggerAnnotations: List<String> = DaggerComponentAnnotations): Boolean {
  val psiIdentifier = this as? PsiIdentifier
  return daggerAnnotations.any { daggerAnnotation ->
    (psiIdentifier?.parent?.parent as? PsiAnnotation)?.qualifiedName == daggerAnnotation
  }
}

fun LeafPsiElement.ktClassOrObject(): KtClassOrObject? {
  val isAClassType = (text == KtTokens.CLASS_KEYWORD.value
      || text == KtTokens.OBJECT_KEYWORD.value
      || text == KtTokens.INTERFACE_KEYWORD.value)
  if (elementType is KtKeywordToken && isAClassType) {
    val classOrObjectCandidate = parent
    if (classOrObjectCandidate is KtClassOrObject) {
      return classOrObjectCandidate
    }
  }
  return null
}

fun KtClassOrObject.hasAnnotation(qualifiedAnnotationName: String): Boolean {
  return findAnnotation(FqName(qualifiedAnnotationName)) != null
}

/**
 * Converts a Kotlin class object to `PsiClass` instance. Null if unsuccessful.
 */
fun KtClassOrObject.toPsiClass(): PsiClass? {
  val javaPsiFacade = JavaPsiFacade.getInstance(project)
  val searchScope = GlobalSearchScope.allScope(project)
  return fqName?.asString()
    ?.let { qualifiedName -> javaPsiFacade.findClass(qualifiedName, searchScope) }
}

fun PsiClass?.isSubClassOf(qualifiedClassName: String): Boolean {
  var currSuperClass = this
  do {
    when (currSuperClass?.qualifiedName) {
      qualifiedClassName -> return true
      else -> currSuperClass = currSuperClass?.superClass
    }
  } while (currSuperClass != null)
  return false
}