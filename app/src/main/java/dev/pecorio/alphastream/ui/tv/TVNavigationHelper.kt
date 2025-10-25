package dev.pecorio.alphastream.ui.tv

import android.content.Context
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.children

/**
 * Helper class pour gérer la navigation TV avec télécommande/joystick
 */
class TVNavigationHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "TVNavigationHelper"
        
        // Codes de touches pour la navigation TV
        const val KEYCODE_DPAD_UP = KeyEvent.KEYCODE_DPAD_UP
        const val KEYCODE_DPAD_DOWN = KeyEvent.KEYCODE_DPAD_DOWN
        const val KEYCODE_DPAD_LEFT = KeyEvent.KEYCODE_DPAD_LEFT
        const val KEYCODE_DPAD_RIGHT = KeyEvent.KEYCODE_DPAD_RIGHT
        const val KEYCODE_DPAD_CENTER = KeyEvent.KEYCODE_DPAD_CENTER
        const val KEYCODE_ENTER = KeyEvent.KEYCODE_ENTER
        const val KEYCODE_BACK = KeyEvent.KEYCODE_BACK
        const val KEYCODE_MENU = KeyEvent.KEYCODE_MENU
    }
    
    /**
     * Configure une vue pour être compatible TV
     */
    fun setupTVCompatibility(view: View) {
        view.apply {
            isFocusable = true
            // Pour les EditText, garder focusableInTouchMode = true pour le mobile
            if (view !is android.widget.EditText) {
                isFocusableInTouchMode = false
            }
            isClickable = true
            
            // Ajouter un listener pour les changements de focus
            setOnFocusChangeListener { v, hasFocus ->
                onFocusChanged(v, hasFocus)
            }
        }
    }
    
    /**
     * Configure spécifiquement un EditText pour être compatible TV et mobile
     */
    fun setupTVEditText(editText: android.widget.EditText) {
        editText.apply {
            isFocusable = true
            isFocusableInTouchMode = true // Important pour le mobile
            isClickable = true
            
            // Ajouter un listener pour les changements de focus
            setOnFocusChangeListener { v, hasFocus ->
                onFocusChanged(v, hasFocus)
                if (hasFocus) {
                    // Assurer que le clavier apparaît sur mobile
                    post {
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                    }
                }
            }
        }
    }
    
    /**
     * Configure un RecyclerView pour la navigation TV
     */
    fun setupTVRecyclerView(recyclerView: RecyclerView) {
        recyclerView.apply {
            // Permettre au RecyclerView de recevoir le focus
            isFocusable = true
            isFocusableInTouchMode = false
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            
            // Améliorer la navigation dans le RecyclerView
            setOnKeyListener { _, keyCode, event ->
                handleRecyclerViewNavigation(this, keyCode, event)
            }
            
            // Listener pour gérer le focus sur le RecyclerView
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    // Quand le RecyclerView reçoit le focus, le donner au premier élément
                    // Mais seulement si aucun enfant n'a déjà le focus
                    post {
                        if (focusedChild == null) {
                            findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Configure un ViewGroup pour la navigation TV
     */
    fun setupTVViewGroup(viewGroup: ViewGroup) {
        viewGroup.apply {
            isFocusable = false
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            
            // Configurer tous les enfants
            children.forEach { child ->
                if (child is ViewGroup) {
                    setupTVViewGroup(child)
                } else if (child !is android.widget.EditText) {
                    // Ne pas configurer les EditText automatiquement
                    setupTVCompatibility(child)
                }
            }
        }
    }
    
    /**
     * Trouve la première vue focusable dans un ViewGroup
     */
    fun findFirstFocusableView(viewGroup: ViewGroup): View? {
        return viewGroup.children.firstOrNull { view ->
            when {
                view.isFocusable -> true
                view is ViewGroup -> findFirstFocusableView(view) != null
                else -> false
            }
        }
    }
    
    /**
     * Demande le focus sur la première vue focusable
     */
    fun requestInitialFocus(rootView: ViewGroup) {
        findFirstFocusableView(rootView)?.requestFocus()
    }
    
    /**
     * Demande le focus sur le premier élément d'un RecyclerView
     */
    fun requestFocusOnFirstItem(recyclerView: RecyclerView) {
        recyclerView.post {
            val adapter = recyclerView.adapter
            if (adapter != null && adapter.itemCount > 0) {
                // Vérifier si un élément a déjà le focus
                if (recyclerView.focusedChild != null) {
                    android.util.Log.d(TAG, "RecyclerView already has focused child, not changing focus")
                    return@post
                }
                
                // Essayer de trouver le premier ViewHolder
                val firstViewHolder = recyclerView.findViewHolderForAdapterPosition(0)
                if (firstViewHolder != null) {
                    firstViewHolder.itemView.requestFocus()
                } else {
                    // Si pas de ViewHolder, donner le focus au RecyclerView qui le transférera
                    recyclerView.requestFocus()
                }
            } else {
                // Pas de contenu, garder le focus sur les boutons de navigation
                android.util.Log.d(TAG, "No content in RecyclerView, keeping focus on navigation")
            }
        }
    }
    
    /**
     * Gère les changements de focus
     */
    private fun onFocusChanged(view: View, hasFocus: Boolean) {
        if (hasFocus) {
            // Assurer que la vue focusée est visible
            ensureViewVisible(view)
            
            // Log pour debug
            android.util.Log.d(TAG, "Focus gained: ${view.javaClass.simpleName}")
        }
    }
    
    /**
     * Assure qu'une vue est visible en scrollant si nécessaire
     */
    private fun ensureViewVisible(view: View) {
        var parent = view.parent
        while (parent != null) {
            when (parent) {
                is RecyclerView -> {
                    val position = parent.getChildAdapterPosition(view)
                    if (position != RecyclerView.NO_POSITION) {
                        parent.smoothScrollToPosition(position)
                    }
                }
                is androidx.core.widget.NestedScrollView -> {
                    parent.smoothScrollTo(0, view.top)
                }
                is android.widget.ScrollView -> {
                    parent.smoothScrollTo(0, view.top)
                }
            }
            parent = parent.parent
        }
    }
    
    /**
     * Gère la navigation dans un RecyclerView
     */
    private fun handleRecyclerViewNavigation(
        recyclerView: RecyclerView, 
        keyCode: Int, 
        event: KeyEvent
    ): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        
        val layoutManager = recyclerView.layoutManager ?: return false
        val focusedChild = recyclerView.focusedChild ?: return false
        val position = recyclerView.getChildAdapterPosition(focusedChild)
        
        if (position == RecyclerView.NO_POSITION) return false
        
        return when (keyCode) {
            KEYCODE_DPAD_UP -> {
                if (position > 0) {
                    recyclerView.smoothScrollToPosition(position - 1)
                    true
                } else false
            }
            KEYCODE_DPAD_DOWN -> {
                val adapter = recyclerView.adapter
                if (adapter != null && position < adapter.itemCount - 1) {
                    recyclerView.smoothScrollToPosition(position + 1)
                    true
                } else false
            }
            else -> false
        }
    }
    
    /**
     * Vérifie si l'appareil est en mode TV
     */
    fun isTVMode(): Boolean {
        val packageManager = context.packageManager
        return packageManager.hasSystemFeature("android.software.leanback") ||
               packageManager.hasSystemFeature("android.hardware.type.television")
    }
    
    /**
     * Vérifie si une touche est une touche de navigation
     */
    fun isNavigationKey(keyCode: Int): Boolean {
        return when (keyCode) {
            KEYCODE_DPAD_UP,
            KEYCODE_DPAD_DOWN,
            KEYCODE_DPAD_LEFT,
            KEYCODE_DPAD_RIGHT,
            KEYCODE_DPAD_CENTER,
            KEYCODE_ENTER -> true
            else -> false
        }
    }
    
    /**
     * Gère les événements de touches globaux
     */
    fun handleGlobalKeyEvent(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        
        return when (keyCode) {
            KEYCODE_BACK -> {
                // Gérer le retour
                android.util.Log.d(TAG, "Back key pressed")
                false // Laisser l'activité gérer
            }
            KEYCODE_MENU -> {
                // Gérer le menu
                android.util.Log.d(TAG, "Menu key pressed")
                false // Laisser l'activité gérer
            }
            else -> false
        }
    }
}