package com.pangea.oriontek.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pangea.oriontek.R
import com.pangea.oriontek.databinding.ActivityHomeBinding
import com.pangea.oriontek.domain.model.Client
import com.pangea.oriontek.ui.common.SpaceItemDecoration
import com.pangea.oriontek.ui.fragments.CreateClientFragment
import com.pangea.oriontek.ui.home.adapter.ClientAdapter
import com.pangea.oriontek.ui.home.states.HomeEvent
import com.pangea.oriontek.ui.home.states.HomeUiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var adapter: ClientAdapter

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        setupToolbar()
        setupRecyclerView()
        setupBackStackListener()
        observeUiState()
        observeEvents()
        newClient()

    }


    private fun setupToolbar() {
        setSupportActionBar(binding.itoolbar.toolbar)
        supportActionBar?.apply {
            title = getString(R.string.title)
            setDisplayHomeAsUpEnabled(false)
        }

    }


    private fun setupBackStackListener() {
        supportFragmentManager.addOnBackStackChangedListener {
            val hasFragments =
                supportFragmentManager.backStackEntryCount > 0

            if (hasFragments) {
                binding.fab.hide()
            } else {
                binding.fab.show()
                supportActionBar?.setDisplayHomeAsUpEnabled(false)
                supportActionBar?.title = getString(R.string.app_name)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = ClientAdapter(
            onClick = { store ->
                openDetail(store)
            },

            onDelete = { store ->
                showOptionsDialog(store)
            }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = this@HomeActivity.adapter
        }

        binding.recyclerView.addItemDecoration(
            SpaceItemDecoration(
                resources.getDimensionPixelSize(R.dimen.spacing_xs)
            )
        )

    }


    // -------------------------
    // Observes
    // -------------------------
    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {

                        is HomeUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.recyclerView.visibility = View.GONE
                        }

                        is HomeUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.recyclerView.visibility = View.VISIBLE

                            adapter.submitList(state.clients)
                        }

                        is HomeUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.recyclerView.visibility = View.VISIBLE

                            Toast.makeText(
                                this@HomeActivity,
                                state.message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is HomeEvent.ShowMessage -> {
                            Toast.makeText(this@HomeActivity, event.resId, Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }

        }
    }

    // Agregar nuevo cliente
    private fun newClient() {
        binding.fab.setOnClickListener {
            launchEditFragment()
        }
    }


    // -------------------------
    // Navigation
    // -------------------------
    private fun openDetail(client: Client) {
        val args = Bundle().apply {
            putLong(getString(R.string.arg_id), client.id)
        }
        launchEditFragment(args)

    }

    private fun launchEditFragment(args: Bundle? = null) {
        val fragment = CreateClientFragment().apply {
            arguments = args
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.main, fragment)
            .addToBackStack(null)
            .commit()

    }

    // -------------------------
    // Dialogs
    // -------------------------
    private fun showOptionsDialog(client: Client) {
        val options = resources.getStringArray(R.array.array_options_item)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_options_title)
            .setItems(options) { _, index ->
                when (index) {
                    0 -> confirmDelete(client)
                    1 -> dial(client.phone)

                }
            }
            .show()
    }


    // Eliminar cliente
    private fun confirmDelete(client: Client) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_delete_title)
            .setPositiveButton(R.string.dialog_delete_confirm) { _, _ ->
                viewModel.delete(client)
            }
            .setNegativeButton(R.string.dialog_delete_cancel, null)
            .show()
    }

    // LLamar al cliente
    private fun dial(phone: String) {
        val intent = Intent(Intent.ACTION_DIAL, "tel:$phone".toUri())
        startIntent(intent)
    }


    private fun startIntent(intent: Intent) {
        runCatching {
            startActivity(intent)
        }.onFailure {
            Toast.makeText(
                this,
                R.string.message_no_compatible_app_found,
                Toast.LENGTH_SHORT
            ).show()
        }
    }


}