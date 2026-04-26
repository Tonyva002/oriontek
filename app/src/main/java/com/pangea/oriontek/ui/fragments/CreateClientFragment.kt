package com.pangea.oriontek.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.textfield.TextInputLayout
import com.pangea.oriontek.R
import com.pangea.oriontek.databinding.FragmentCreateClientBinding
import com.pangea.oriontek.domain.model.Address
import com.pangea.oriontek.domain.model.Client
import com.pangea.oriontek.domain.model.ClientWithAddresses
import com.pangea.oriontek.ui.fragments.states.CreateClientEvent
import com.pangea.oriontek.ui.fragments.states.CreateClientUiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CreateClientFragment : Fragment() {

    private lateinit var binding: FragmentCreateClientBinding

    private val viewModel: CreateClientViewModel by viewModels()

    private var original: ClientWithAddresses? = null
    private var client: Client = Client()

    private lateinit var menuProvider: MenuProvider

    private var isEditMode = false

    private var isCleaning = false

    private var indexPhoto = 0


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
        super.onViewCreated(view, savedInstanceState)

        val id = arguments?.getLong(getString(R.string.arg_id), 0L) ?: 0L
        isEditMode = id != 0L

        setupActionBar()
        setupTextFields()
        setupMenu()
        setupListeners()

        if (isEditMode) {
            viewModel.loadClient(id)
        } else {
            client = Client()
        }

        observeUiState()
        observeEvents()
    }

    // -------------------------
    // Observers
    // -------------------------
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is CreateClientUiState.Success -> {
                            original = state.client
                            client = state.client.client
                            fillFields(state.client)
                        }

                        else -> Unit
                    }
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is CreateClientEvent.ShowMessage -> {
                            Toast.makeText(requireContext(), event.resId, Toast.LENGTH_SHORT).show()
                        }

                        CreateClientEvent.Created -> {
                            Toast.makeText(
                                requireContext(),
                                R.string.message_created_success,
                                Toast.LENGTH_SHORT
                            ).show()
                            clearFields()
                            client = Client()
                            original = null
                        }

                        CreateClientEvent.Updated -> {
                            Toast.makeText(
                                requireContext(),
                                R.string.message_updated_success,
                                Toast.LENGTH_SHORT
                            ).show()
                            requireActivity().supportFragmentManager.popBackStack()
                        }
                    }
                }
            }
        }
    }

    // -------------------------
    // Configuracion de la UI
    // -------------------------

    private fun setupActionBar() {
        val activity = activity as? AppCompatActivity ?: return

        activity.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setTitle(
                if (isEditMode) {
                    R.string.update_client
                } else {
                    R.string.create_client
                }
            )
        }
    }

    private fun setupListeners() {
        binding.btnChangePhoto.setOnClickListener {
            selectPhoto()
        }
    }

    private fun setupMenu() {
        menuProvider = object : MenuProvider {

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {

                    R.id.action_save -> {
                        val isValid = validateFields(
                            binding.tilName,
                            binding.tilLastname,
                            binding.tilEmail,
                            binding.tilCompany,
                            binding.tilPhone,
                            binding.tilAddress
                        )
                        if (isValid) saveClient()
                        true
                    }

                    android.R.id.home -> {
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                        true
                    }

                    else -> false
                }
            }
        }

        requireActivity().addMenuProvider(
            menuProvider,
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }


    // Guardar cliente
    private fun saveClient() {

        val updatedClient = client.copy(
            name = binding.etName.text.toString().trim(),
            lastName = binding.etLastname.text.toString().trim(),
            company = binding.etCompany.text.toString().trim(),
            email = binding.etEmail.text.toString().trim(),
            phone = binding.etPhone.text.toString().trim(),
            photoResId = photos[indexPhoto]
        )

        val addresses = mutableListOf<Address>()

        val address1 = binding.etAddress.text.toString().trim()
        val address2 = binding.etAddress2.text.toString().trim()

        if (address1.isNotEmpty()) {
            addresses.add(
                Address(
                    id = 0,
                    fullAddress = address1,
                    clientId = updatedClient.id
                )
            )
        }

        if (address2.isNotEmpty()) {
            addresses.add(
                Address(
                    id = 0,
                    fullAddress = address2,
                    clientId = updatedClient.id
                )
            )
        }

        viewModel.saveClient(
            original = original,
            updatedClient = updatedClient,
            updatedAddresses = addresses
        )
    }


    // Llenar los campos de la UI
    private fun fillFields(data: ClientWithAddresses) = with(binding) {
        val client = data.client

        etName.setText(client.name)
        etLastname.setText(client.lastName)
        etCompany.setText(client.company)
        etEmail.setText(client.email)
        etPhone.setText(client.phone)

        when (data.addresses.size) {
            1 -> etAddress.setText(data.addresses[0].fullAddress)
            2 -> {
                etAddress.setText(data.addresses[0].fullAddress)
                etAddress2.setText(data.addresses[1].fullAddress)
            }
        }

        indexPhoto = photos.indexOf(client.photoResId).takeIf { it != -1 } ?: 0

        val photo = if (client.photoResId != 0) {
            client.photoResId
        } else {
            R.drawable.photo_01
        }

        loadImage(photo)
    }

    private fun loadImage(photoResId: Int) {
        Glide.with(requireContext())
            .load(photoResId)
            .placeholder(R.drawable.photo_01)
            .error(R.drawable.photo_01)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .into(binding.imgPhoto)
    }

    // -------------------------
    // VALIDATION
    // -------------------------
    private fun setupTextFields() = with(binding) {
        val fields = listOf(
            etName to tilName,
            etLastname to tilLastname,
            etCompany to tilCompany,
            etEmail to tilEmail,
            etPhone to tilPhone,
            etAddress to tilAddress

        )

        fields.forEach { (editText, layout) ->
            editText.doAfterTextChanged {
                if (!isCleaning) {
                    validateFields(layout)
                }
            }
        }
    }

    private fun validateFields(vararg fields: TextInputLayout): Boolean {
        var isValid = true

        fields.forEach { field ->
            val value = field.editText?.text.toString().trim()

            if (value.isEmpty()) {
                field.error = getString(R.string.required)
                isValid = false
            } else {
                field.error = null
            }
        }

        if (!isValid) {
            Toast.makeText(requireContext(), R.string.message_field_valid, Toast.LENGTH_SHORT)
                .show()
        }

        return isValid
    }


    // Seleccionar la foto
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
                    "Foto 6",
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
        isCleaning = true

        etName.text?.clear()
        etLastname.text?.clear()
        etCompany.text?.clear()
        etEmail.text?.clear()
        etPhone.text?.clear()
        etAddress.text?.clear()
        etAddress2.text?.clear()

        // Limpiar errores
        tilName.error = null
        tilLastname.error = null
        tilCompany.error = null
        tilEmail.error = null
        tilPhone.error = null
        tilAddress.error = null

        // Reset imagen
        indexPhoto = 0
        imgPhoto.setImageResource(R.drawable.photo_01)

        isCleaning = false
    }

    override fun onDestroyView() {
        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
            title = getString(R.string.app_name)
        }
        super.onDestroyView()
    }
}