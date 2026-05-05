package com.pangea.oriontek.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.pangea.oriontek.R
import com.pangea.oriontek.databinding.FragmentCreateClientBinding
import com.pangea.oriontek.domain.model.Address
import com.pangea.oriontek.domain.model.Client
import com.pangea.oriontek.ui.fragments.states.CreateClientEvent
import com.pangea.oriontek.ui.fragments.states.CreateClientUiState
import com.pangea.oriontek.ui.fragments.states.ValidationErrors
import com.pangea.oriontek.ui.home.HomeActivity.Companion.ARG_ID
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class CreateClientFragment : Fragment() {

    private lateinit var binding: FragmentCreateClientBinding
    private val viewModel: CreateClientViewModel by viewModels()

    private var indexPhoto = 0

    private var isFirstLoad = true

    private val photos = arrayOf(
        R.drawable.photo_01,
        R.drawable.photo_02,
        R.drawable.photo_03,
        R.drawable.photo_04,
        R.drawable.photo_05,
        R.drawable.photo_06,
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCreateClientBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val id = arguments?.getLong(ARG_ID, 0L) ?: 0L

        setupActionBar(id != 0L)
        setupMenu()
        setupListeners()
        setupTextWatchers() // 👈 NUEVO

        if (id != 0L) viewModel.loadClient(id)

        observeUiState()
        observeEvents()
    }

    // -------------------------
    // OBSERVERS
    // -------------------------
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    when (state) {

                        is CreateClientUiState.Loading -> {

                        }

                        is CreateClientUiState.Form -> {

                            if (isFirstLoad) {
                                fillFields(
                                    client = state.data.client,
                                    addresses = state.data.addresses
                                )
                                isFirstLoad = false
                            }

                            showErrors(state.errors)
                        }

                        is CreateClientUiState.Error -> {
                            Toast.makeText(
                                requireContext(),
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
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {

                        is CreateClientEvent.ShowMessage -> {
                            Toast.makeText(
                                requireContext(),
                                event.resId,
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        CreateClientEvent.Created -> {
                            Toast.makeText(
                                requireContext(),
                                R.string.message_created_success,
                                Toast.LENGTH_SHORT
                            ).show()
                            clearFields()
                        }

                        CreateClientEvent.Updated -> {
                            Toast.makeText(
                                requireContext(),
                                R.string.message_updated_success,
                                Toast.LENGTH_SHORT
                            ).show()
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }

                    }
                }
            }
        }
    }


    // ActionBar (Crear cliente o actualizar cliente)
    private fun setupActionBar(isEditMode: Boolean) {
        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setTitle(
                if (isEditMode) R.string.update_client
                else R.string.create_client
            )
        }
    }

    // Menu
    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {

            override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
                inflater.inflate(R.menu.menu, menu)
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                return when (item.itemId) {

                    R.id.action_save -> {
                        saveClient()
                        true
                    }

                    android.R.id.home -> {
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                        true
                    }

                    else -> false
                }
            }

        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupListeners() {
        binding.btnChangePhoto.setOnClickListener {
            selectPhoto()
        }
    }

    //Limpiar errores automáticamente
    private fun setupTextWatchers() = with(binding) {
        val fields = listOf(
            etName to tilName,
            etLastname to tilLastname,
            etEmail to tilEmail,
            etCompany to tilCompany,
            etPhone to tilPhone,
            etAddress to tilAddress
        )

        fields.forEach { (editText, layout) ->
            editText.doAfterTextChanged {
                layout.error = null
            }
        }
    }


    // Guardar cliente (Guardar o actualizar de maneja en el viewmodel)
    private fun saveClient() {
        viewModel.saveClient(
            name = binding.etName.text.toString(),
            lastName = binding.etLastname.text.toString(),
            email = binding.etEmail.text.toString(),
            company = binding.etCompany.text.toString(),
            phone = binding.etPhone.text.toString(),
            address1 = binding.etAddress.text.toString(),
            address2 = binding.etAddress2.text.toString(),
            photoResId = photos[indexPhoto]
        )
    }


    // Mostrar errores en los campos
    private fun showErrors(e: ValidationErrors) = with(binding) {
        tilName.error = e.name
        tilLastname.error = e.lastName
        tilEmail.error = e.email
        tilCompany.error = e.company
        tilPhone.error = e.phone
        tilAddress.error = e.address
    }


    // Llenar los campos
    private fun fillFields(
        client: Client,
        addresses: List<Address>
    ) = with(binding) {

        etName.setText(client.name)
        etLastname.setText(client.lastName)
        etCompany.setText(client.company)
        etEmail.setText(client.email)
        etPhone.setText(client.phone)

        etAddress.setText(addresses.getOrNull(0)?.fullAddress.orEmpty())
        etAddress2.setText(addresses.getOrNull(1)?.fullAddress.orEmpty())

        indexPhoto = photos.indexOf(client.photoResId)
            .takeIf { it != -1 } ?: 0

        imgPhoto.setImageResource(photos[indexPhoto])
    }


    // Seleccionar foto
    private fun selectPhoto() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.select_image))
            .setItems(
                arrayOf(
                    "Foto 1",
                    "Foto 2",
                    "Foto 3",
                    "Foto 4",
                    "Foto 5",
                    "Foto 6"
                )
            ) { _, which ->
                indexPhoto = which
                binding.imgPhoto.setImageResource(photos[indexPhoto])
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }


    // Limpiar los campos
    private fun clearFields() = with(binding) {
        etName.text?.clear()
        etLastname.text?.clear()
        etCompany.text?.clear()
        etEmail.text?.clear()
        etPhone.text?.clear()
        etAddress.text?.clear()
        etAddress2.text?.clear()

        // limpiar errores
        tilName.error = null
        tilLastname.error = null
        tilEmail.error = null
        tilCompany.error = null
        tilPhone.error = null
        tilAddress.error = null

        indexPhoto = 0
        imgPhoto.setImageResource(R.drawable.photo_01)
    }
}