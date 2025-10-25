package dev.pecorio.alphastream.ui.tv

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import dev.pecorio.alphastream.R
import dev.pecorio.alphastream.data.model.Movie
import dev.pecorio.alphastream.data.model.Series
import dev.pecorio.alphastream.databinding.ActivityTvMainBinding
import dev.pecorio.alphastream.ui.details.MovieDetailsActivity
import dev.pecorio.alphastream.ui.series.details.SeriesDetailsActivity
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TVMainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityTvMainBinding
    private val viewModel: TVMainViewModel by viewModels()
    private lateinit var tvNavigationHelper: TVNavigationHelper
    private lateinit var contentAdapter: TVContentAdapter
    
    private var currentCategory = "all"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTvMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupTVNavigation()
        setupUI()
        setupObservers()
        
        // Charger le contenu initial
        viewModel.loadContent()
    }
    
    private fun setupTVNavigation() {
        tvNavigationHelper = TVNavigationHelper(this)
        
        // Configurer la navigation TV pour toute l'interface
        tvNavigationHelper.setupTVViewGroup(binding.root)
        
        // Configurer le RecyclerView
        tvNavigationHelper.setupTVRecyclerView(binding.contentGrid)
    }
    
    private fun setupUI() {
        // Configuration du titre
        binding.tvTitle.text = getString(R.string.app_name)
        binding.tvSubtitle.text = "Interface Télévision"
        
        // Configuration de la navigation par catégories
        setupCategoryNavigation()
        
        // Configuration de la grille de contenu
        setupContentGrid()
    }
    
    private fun setupCategoryNavigation() {
        binding.apply {
            // Configurer les boutons de navigation
            listOf(btnAll, btnMovies, btnSeries, btnFavorites).forEach { button ->
                tvNavigationHelper.setupTVCompatibility(button)
            }
            
            // Configurer la navigation horizontale entre les boutons
            setupHorizontalNavigation()
            
            // Listeners pour les catégories
            btnAll.setOnClickListener { 
                selectCategory("all")
                viewModel.loadContent()
            }
            
            btnMovies.setOnClickListener { 
                selectCategory("movies")
                viewModel.loadMovies()
            }
            
            btnSeries.setOnClickListener { 
                selectCategory("series")
                viewModel.loadSeries()
            }
            
            btnFavorites.setOnClickListener { 
                selectCategory("favorites")
                viewModel.loadFavorites()
            }
            
            // Sélectionner "Tout" par défaut
            selectCategory("all")
        }
    }
    
    private fun setupContentGrid() {
        contentAdapter = TVContentAdapter(
            onItemClick = { item -> handleItemClick(item) },
            onItemFocus = { item -> handleItemFocus(item) }
        )
        
        binding.contentGrid.apply {
            layoutManager = GridLayoutManager(this@TVMainActivity, 4).apply {
                // Améliorer la navigation dans la grille
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int = 1
                }
            }
            adapter = contentAdapter
            
            // Optimisations pour TV
            setHasFixedSize(true)
            isNestedScrollingEnabled = true
        }
    }
    
    private fun setupObservers() {
        viewModel.content.observe(this) { content ->
            contentAdapter.submitList(content) {
                // Callback appelé quand la liste est mise à jour
                if (content.isNotEmpty()) {
                    // Ne pas changer le focus automatiquement si l'utilisateur navigue
                    // Seulement donner le focus si aucun élément n'a le focus
                    if (currentFocus == null) {
                        binding.btnAll.requestFocus()
                    }
                } else {
                    // Pas de contenu, s'assurer que le focus reste sur la navigation
                    if (currentFocus == null || !isOnNavigationButtons()) {
                        binding.btnAll.requestFocus()
                    }
                }
            }
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            binding.loadingIndicator.visibility = if (isLoading) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
        }
        
        viewModel.error.observe(this) { error ->
            if (error != null) {
                // Afficher l'erreur
                binding.errorMessage.text = error
                binding.errorMessage.visibility = android.view.View.VISIBLE
            } else {
                binding.errorMessage.visibility = android.view.View.GONE
            }
        }
    }
    
    private fun selectCategory(category: String) {
        currentCategory = category
        
        // Mettre à jour l'apparence des boutons
        binding.apply {
            listOf(
                btnAll to "all",
                btnMovies to "movies", 
                btnSeries to "series",
                btnFavorites to "favorites"
            ).forEach { (button, cat) ->
                button.isSelected = cat == category
            }
        }
        
        // Mettre à jour le sous-titre
        binding.tvSubtitle.text = when (category) {
            "all" -> "Tout le contenu"
            "movies" -> "Films"
            "series" -> "Séries"
            "favorites" -> "Favoris"
            else -> "Interface Télévision"
        }
    }
    
    private fun handleItemClick(item: Any) {
        when (item) {
            is Movie -> {
                val intent = MovieDetailsActivity.newIntent(this, item)
                startActivity(intent)
            }
            is Series -> {
                val intent = SeriesDetailsActivity.newIntent(this, item.getSeriesId(), item.getDisplayTitle())
                startActivity(intent)
            }
        }
    }
    
    private fun handleItemFocus(item: Any) {
        // Optionnel : afficher des informations supplémentaires sur l'élément focusé
        when (item) {
            is Movie -> {
                android.util.Log.d("TVMain", "Focused on movie: ${item.getDisplayTitle()}")
            }
            is Series -> {
                android.util.Log.d("TVMain", "Focused on series: ${item.getDisplayTitle()}")
            }
        }
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        event?.let {
            // Gérer les touches spéciales
            if (tvNavigationHelper.handleGlobalKeyEvent(keyCode, it)) {
                return true
            }
            
            // Gérer les touches de navigation
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> {
                    return handleUpNavigation()
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    return handleDownNavigation()
                }
                else -> {
                    // Autres touches, laisser le comportement par défaut
                }
            }
        }
        
        return super.onKeyDown(keyCode, event)
    }
    
    private fun handleUpNavigation(): Boolean {
        // Si on est dans la grille de contenu, vérifier si on est sur la première ligne
        val focusedChild = binding.contentGrid.focusedChild
        if (focusedChild != null) {
            val position = binding.contentGrid.getChildAdapterPosition(focusedChild)
            if (position in 0..3) { // Première ligne (4 colonnes)
                // Aller aux boutons de navigation
                when (currentCategory) {
                    "all" -> binding.btnAll.requestFocus()
                    "movies" -> binding.btnMovies.requestFocus()
                    "series" -> binding.btnSeries.requestFocus()
                    "favorites" -> binding.btnFavorites.requestFocus()
                    else -> binding.btnAll.requestFocus()
                }
                return true
            }
        }
        
        // Si on est sur les boutons de navigation, aller au titre (pour accéder à la recherche)
        if (isOnNavigationButtons()) {
            // Pour l'instant, rester sur la navigation
            // TODO: Ajouter une barre de recherche accessible
            return false
        }
        
        return false
    }
    
    private fun handleDownNavigation(): Boolean {
        // Si on est sur les boutons de navigation, aller au contenu
        if (isOnNavigationButtons()) {
            // Vérifier s'il y a du contenu avant de naviguer
            val adapter = binding.contentGrid.adapter
            if (adapter != null && adapter.itemCount > 0) {
                tvNavigationHelper.requestFocusOnFirstItem(binding.contentGrid)
                return true
            } else {
                // Pas de contenu, rester sur la navigation
                android.util.Log.d("TVMain", "No content available, staying on navigation")
                return false
            }
        }
        return false
    }
    
    private fun isOnNavigationButtons(): Boolean {
        return binding.btnAll.hasFocus() || 
               binding.btnMovies.hasFocus() || 
               binding.btnSeries.hasFocus() || 
               binding.btnFavorites.hasFocus()
    }
    
    private fun setupHorizontalNavigation() {
        binding.apply {
            // Configuration de la navigation horizontale entre les boutons
            btnAll.nextFocusRightId = btnMovies.id
            btnMovies.nextFocusLeftId = btnAll.id
            btnMovies.nextFocusRightId = btnSeries.id
            btnSeries.nextFocusLeftId = btnMovies.id
            btnSeries.nextFocusRightId = btnFavorites.id
            btnFavorites.nextFocusLeftId = btnSeries.id
            
            // Navigation verticale : vers le bas = contenu
            listOf(btnAll, btnMovies, btnSeries, btnFavorites).forEach { button ->
                button.nextFocusDownId = contentGrid.id
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // Demander le focus initial si aucun élément n'est focusé
        if (currentFocus == null) {
            binding.btnAll.requestFocus()
        }
    }
}